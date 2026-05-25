package sk.ainet.apps.kllama.chat.playground

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.Buffer
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.llm.generate
import sk.ainet.apps.llm.tokenizer.GGUFTokenizer
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.qwen.QwenNetworkLoader

sealed interface QwenLoadingState {
    data object Idle : QwenLoadingState
    data class Loading(val phase: String) : QwenLoadingState
    data class Ready(
        val tokenizer: Tokenizer,
        val runtime: OptimizedLLMRuntime<FP32>,
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

    private val ctx = DirectCpuExecutionContext()

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

        _state.value = QwenLoadingState.Ready(
            tokenizer = tokenizer,
            runtime = runtime,
            loadMillis = startMs.elapsedNow().inWholeMilliseconds,
        )
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
