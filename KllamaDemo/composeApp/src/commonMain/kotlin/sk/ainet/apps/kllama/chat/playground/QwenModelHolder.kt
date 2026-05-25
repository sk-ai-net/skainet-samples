package sk.ainet.apps.kllama.chat.playground

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.Buffer
import sk.ainet.apps.kllama.chat.playground.explainer.ArchitectureSummary
import sk.ainet.apps.kllama.chat.playground.explainer.InstrumentedExecutionContext
import sk.ainet.apps.kllama.chat.playground.explainer.StepSnapshot
import sk.ainet.apps.kllama.chat.playground.explainer.TopKEntry
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.llm.generate
import sk.ainet.apps.llm.tokenizer.GGUFTokenizer
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.nn.topology.ModuleNode
import sk.ainet.lang.types.FP32
import sk.ainet.models.qwen.QwenNetworkLoader

sealed interface QwenLoadingState {
    data object Idle : QwenLoadingState
    data class Loading(val phase: String) : QwenLoadingState
    data class Ready(
        val tokenizer: Tokenizer,
        val runtime: OptimizedLLMRuntime<FP32>,
        val architecture: ArchitectureSummary,
        val loadMillis: Long,
    ) : QwenLoadingState
    data class Failed(val stage: String, val message: String) : QwenLoadingState
}

/**
 * Single-instance holder for the loaded Qwen3-0.6B model. All five
 * playground tabs share the one model — load once, use many times.
 *
 * Tokenizer is parsed first (fast, ~seconds) so the Tokenizer tab can
 * be interactive while the model weights are still loading.
 */
class QwenModelHolder {
    private val _state = MutableStateFlow<QwenLoadingState>(QwenLoadingState.Idle)
    val state: StateFlow<QwenLoadingState> = _state.asStateFlow()

    private val instrumented = InstrumentedExecutionContext()
    private val ctx = instrumented.ctx

    suspend fun load(bytes: ByteArray) {
        if (_state.value is QwenLoadingState.Ready) return
        val startMs = kotlin.time.TimeSource.Monotonic.markNow()

        _state.value = QwenLoadingState.Loading("Reading tokenizer from GGUF metadata...")
        val tokenizer = try {
            val source = Buffer().apply { write(bytes) }
            GGUFTokenizer.fromSource(source)
        } catch (e: Throwable) {
            _state.value = QwenLoadingState.Failed(
                stage = "tokenizer",
                message = "${e::class.simpleName}: ${e.message ?: "no message"}",
            )
            return
        }

        _state.value = QwenLoadingState.Loading("Loading 600M-parameter weights (this can take ~40s)...")
        val runtime = try {
            val sourceProvider = { Buffer().apply { write(bytes) } }
            val model = QwenNetworkLoader
                .fromGguf(sourceProvider, QuantPolicy.DEQUANTIZE_TO_FP32)
                .load<FP32, Float>(ctx)

            OptimizedLLMRuntime(
                model = model,
                ctx = ctx,
                mode = OptimizedLLMMode.DIRECT,
                dtype = FP32::class,
                bos = tokenizer.bosTokenId,
            )
        } catch (e: Throwable) {
            _state.value = QwenLoadingState.Failed(
                stage = "model-load",
                message = "${e::class.simpleName}: ${e.message ?: "no message"}",
            )
            return
        }

        val architecture = summarizeArchitecture(runtime)

        _state.value = QwenLoadingState.Ready(
            tokenizer = tokenizer,
            runtime = runtime,
            architecture = architecture,
            loadMillis = startMs.elapsedNow().inWholeMilliseconds,
        )
    }

    /**
     * Run one `runtime.forward` step under instrumentation. The hooks +
     * observer in [instrumented] capture per-layer residuals and attention
     * heatmaps, which are bundled into a [StepSnapshot] together with the
     * sampled next token and a top-k of the logits.
     *
     * The runtime's internal position counter still advances; the caller
     * is responsible for resetting the runtime (or building a fresh one)
     * between explainer sessions.
     */
    fun stepInstrumented(
        tokenId: Int,
        priorTokens: IntArray,
        temperature: Float,
    ): StepSnapshot {
        val ready = _state.value as? QwenLoadingState.Ready ?: error("Model not loaded")
        instrumented.reset()
        instrumented.captureEnabled = true
        val startMs = kotlin.time.TimeSource.Monotonic.markNow()
        val logits = try {
            ready.runtime.forward(tokenId)
        } finally {
            instrumented.captureEnabled = false
        }
        val elapsed = startMs.elapsedNow().inWholeMilliseconds

        val logitVec = logits.data.copyToFloatArray()
        val sampledId = if (temperature <= 1e-6f) {
            argmax(logitVec)
        } else {
            sampleSoftmax(logitVec, temperature)
        }
        val topK = topKEntries(logitVec, k = 10, tokenizer = ready.tokenizer, temperature = temperature)

        return StepSnapshot(
            inputTokenId = tokenId,
            tokensSoFar = priorTokens + sampledId,
            sampledTokenId = sampledId,
            sampledTokenText = ready.tokenizer.decode(sampledId),
            topK = topK,
            perLayerAttention = instrumented.attentionCaptures(),
            perLayerResidual = instrumented.residualCaptures(),
            elapsedMillis = elapsed,
        )
    }

    private fun summarizeArchitecture(runtime: OptimizedLLMRuntime<FP32>): ArchitectureSummary {
        val blockNames = mutableListOf<String>()
        walkModules(runtime.modelRoot()) { node ->
            if (node.name.startsWith("blk.")) blockNames += node.name
        }
        return ArchitectureSummary(
            numLayers = runtime.nLayers,
            hiddenDim = runtime.dim,
            vocabSize = runtime.vocabSize,
            maxSeqLen = runtime.seqLen,
            blockNames = blockNames.distinct().sortedBy { it.removePrefix("blk.").toIntOrNull() ?: 0 },
        )
    }

    private fun walkModules(root: ModuleNode, visit: (ModuleNode) -> Unit) {
        visit(root)
        root.children.forEach { walkModules(it, visit) }
    }

    private fun OptimizedLLMRuntime<FP32>.modelRoot(): ModuleNode {
        // OptimizedLLMRuntime keeps `model` private; reach in via reflection-free
        // workaround: the module hierarchy is exposed indirectly through the
        // runtime's public APIs (nLayers / dim / etc.), but for a name walk we
        // need the actual root. Until upstream exposes it, fall back to a
        // shallow synthetic node containing the block names we already know.
        return SyntheticRoot(numLayers = this.nLayers)
    }

    private class SyntheticRoot(numLayers: Int) : ModuleNode {
        override val id: String = "root"
        override val name: String = "qwen3"
        override var path: String? = null
        override val children: List<ModuleNode> =
            (0 until numLayers).map { idx -> SyntheticChild("blk.$idx") }
        override val params: List<sk.ainet.lang.nn.topology.ModuleParameter<*, *>> = emptyList()
    }

    private class SyntheticChild(override val name: String) : ModuleNode {
        override val id: String = name
        override var path: String? = null
        override val children: List<ModuleNode> = emptyList()
        override val params: List<sk.ainet.lang.nn.topology.ModuleParameter<*, *>> = emptyList()
    }

    private fun argmax(values: FloatArray): Int {
        var bestIdx = 0
        var bestVal = values[0]
        for (i in 1 until values.size) {
            if (values[i] > bestVal) { bestVal = values[i]; bestIdx = i }
        }
        return bestIdx
    }

    private fun sampleSoftmax(values: FloatArray, temperature: Float): Int {
        val probs = softmax(values, temperature)
        val u = kotlin.random.Random.nextFloat()
        var acc = 0f
        for (i in probs.indices) {
            acc += probs[i]
            if (u <= acc) return i
        }
        return probs.size - 1
    }

    private fun softmax(values: FloatArray, temperature: Float): FloatArray {
        val out = FloatArray(values.size)
        var maxV = Float.NEGATIVE_INFINITY
        for (v in values) if (v > maxV) maxV = v
        var sum = 0f
        for (i in values.indices) {
            val e = kotlin.math.exp(((values[i] - maxV) / temperature).toDouble()).toFloat()
            out[i] = e
            sum += e
        }
        if (sum > 0f) for (i in out.indices) out[i] /= sum
        return out
    }

    private fun topKEntries(
        logits: FloatArray,
        k: Int,
        tokenizer: Tokenizer,
        temperature: Float,
    ): List<TopKEntry> {
        val effectiveT = if (temperature <= 1e-6f) 1f else temperature
        val probs = softmax(logits, effectiveT)
        val indexed = probs.withIndex().sortedByDescending { it.value }.take(k)
        return indexed.map { (idx, prob) ->
            TopKEntry(id = idx, text = tokenizer.decode(idx), probability = prob)
        }
    }

    /**
     * Stream a continuation given an already-tokenized prompt. Emits decoded
     * text chunks via [onChunk]. Caller is responsible for prepending BOS,
     * applying chat templates, etc.
     */
    suspend fun stream(
        promptTokens: IntArray,
        maxTokens: Int,
        temperature: Float,
        onChunk: (String) -> Unit,
    ) {
        val ready = _state.value as? QwenLoadingState.Ready ?: error("Model not loaded")
        ready.runtime.generate(
            prompt = promptTokens,
            steps = maxTokens,
            temperature = temperature,
        ) { id ->
            onChunk(ready.tokenizer.decode(id))
        }
    }
}
