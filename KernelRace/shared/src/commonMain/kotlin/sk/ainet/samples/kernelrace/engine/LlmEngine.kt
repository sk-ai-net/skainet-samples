package sk.ainet.samples.kernelrace.engine

import kotlin.math.round
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import sk.ainet.apps.kllama.agent.generateUntilStop
import sk.ainet.context.ExecutionContext
import sk.ainet.samples.kernelrace.model.ModelData
import sk.ainet.samples.kernelrace.platform.logEvent

/** What [sk.ainet.samples.kernelrace.vm.ChatViewModel] needs from an engine — lets tests
 *  substitute a fake without touching SKaiNET's own (private-constructor) [LlmEngine]. */
interface GenerativeEngine {
    suspend fun generate(prompt: String, maxTokens: Int = 200, onToken: (String) -> Unit): String
}

/**
 * Loads a GGUF LLM and streams generated tokens — the whole integration.
 *
 * On Android this is the Scene 4 shot of the demo video: GGUF in, tokens out, no Python,
 * no C++ in the app build. The hand-written ARM NEON kernels (skainet-backend-jni-cpu)
 * register themselves through the kernel SPI just by being on the classpath.
 */
class LlmEngine private constructor(private val components: LlamaComponents) : GenerativeEngine {

    private val tokenizer get() = components.tokenizer
    private val runtime get() = components.runtime

    override suspend fun generate(prompt: String, maxTokens: Int, onToken: (String) -> Unit): String {
        runtime.reset()
        val templated = chatMlEnvelope(prompt)
        val promptTokens = tokenizer.encode(templated)
        logEvent(
            "generate_start",
            "promptTokens" to promptTokens.size,
            "maxTokens" to maxTokens,
            "eos" to tokenizer.eosTokenId,
        )
        val genStart = TimeSource.Monotonic.markNow()
        var firstTokenAt: TimeMark? = null
        var tokenCount = 0
        val result = runtime.generateUntilStop(
            prompt = promptTokens,
            maxTokens = maxTokens,
            eosTokenId = tokenizer.eosTokenId,
            temperature = 0.7f,
            onToken = { tokenId ->
                if (tokenCount == 0) firstTokenAt = TimeSource.Monotonic.markNow()
                tokenCount++
                onToken(tokenizer.decode(tokenId))
            },
            decode = { tokenizer.decode(it) },
        )
        val totalMs = genStart.elapsedNow().inWholeMilliseconds
        val decodeS = firstTokenAt?.elapsedNow()?.inWholeMilliseconds?.let { it / 1000.0 } ?: 0.0
        logEvent(
            "generate_done",
            "tokens" to tokenCount,
            "totalMs" to totalMs,
            "decodeTokPerSec" to if (tokenCount > 1 && decodeS > 0) {
                formatFixed2((tokenCount - 1) / decodeS)
            } else null,
            "textLen" to result.text.length,
        )
        return result.text
    }

    companion object {
        /** Call off the main thread — streams weights, never materializes the whole file on-heap. */
        suspend fun load(ctx: ExecutionContext, model: ModelData): LlmEngine =
            LlmEngine(buildLlamaComponents(ctx, model))
    }
}

/** `"%.2f".format` isn't available in common code — round to 2 decimals by hand. */
private fun formatFixed2(value: Double): String {
    val rounded = round(value * 100) / 100
    val whole = rounded.toLong()
    val frac = round((rounded - whole) * 100).toInt().let { if (it < 0) -it else it }
    return "$whole.${frac.toString().padStart(2, '0')}"
}
