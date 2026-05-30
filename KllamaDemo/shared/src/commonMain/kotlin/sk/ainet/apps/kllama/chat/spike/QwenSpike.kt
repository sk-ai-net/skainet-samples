package sk.ainet.apps.kllama.chat.spike

import kotlinx.io.Buffer
import kotlin.time.TimeSource
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.generate
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.qwen.QwenNetworkLoader

sealed interface QwenSpikeResult {
    data class Success(
        val tokenIds: List<Int>,
        val loadMillis: Long,
        val generateMillis: Long,
    ) : QwenSpikeResult

    data class Failure(val stage: String, val message: String) : QwenSpikeResult
}

/**
 * Phase-0 smoke: build a Qwen model from raw GGUF bytes, wrap it in the
 * unified runtime, generate 5 tokens starting from a single BOS token.
 * Tokenizer is intentionally skipped — this test only proves the
 * model + runtime pipeline works on the current target.
 */
suspend fun runQwenSpike(modelBytes: ByteArray): QwenSpikeResult {
    val ctx = DirectCpuExecutionContext()
    val timeSource = TimeSource.Monotonic

    val model = try {
        val loadStart = timeSource.markNow()
        val sourceProvider = { Buffer().apply { write(modelBytes) } }
        val m = QwenNetworkLoader
            .fromGguf(sourceProvider, QuantPolicy.DEQUANTIZE_TO_FP32)
            .load<FP32, Float>(ctx)
        loadStart to m
    } catch (e: Throwable) {
        return QwenSpikeResult.Failure(
            stage = "model-load",
            message = "${e::class.simpleName}: ${e.message ?: "no message"}",
        )
    }

    val (loadStart, qwen) = model
    val loadMillis = loadStart.elapsedNow().inWholeMilliseconds

    return try {
        val runtime = OptimizedLLMRuntime(
            model = qwen,
            ctx = ctx,
            mode = OptimizedLLMMode.DIRECT,
            dtype = FP32::class,
            bos = 1,
        )

        val tokens = mutableListOf<Int>()
        val genStart = timeSource.markNow()
        runtime.generate(
            prompt = intArrayOf(1),
            steps = 5,
            temperature = 0f,
        ) { id -> tokens += id }
        val genMillis = genStart.elapsedNow().inWholeMilliseconds

        QwenSpikeResult.Success(
            tokenIds = tokens.toList(),
            loadMillis = loadMillis,
            generateMillis = genMillis,
        )
    } catch (e: Throwable) {
        QwenSpikeResult.Failure(
            stage = "runtime-or-generate",
            message = "${e::class.simpleName}: ${e.message ?: "no message"}",
        )
    }
}
