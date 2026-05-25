package sk.ainet.apps.kllama.chat.playground

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.llm.generate
import sk.ainet.lang.types.FP32

/**
 * Generate up to [maxTokens] tokens, stopping early when the model emits
 * the `<|im_end|>` ChatML control token. Streams decoded text chunks via
 * [onText]. The buffer dance is so we can detect `<|im_end|>` across token
 * boundaries (it tokenizes as multiple pieces on Qwen3).
 *
 * Runs the inference loop on [Dispatchers.Default] so the UI thread stays
 * responsive on JVM/Android/iOS. The [onText] callback is invoked on the
 * Default dispatcher; Compose's mutableStateOf is safe to write from any
 * thread.
 */
suspend fun OptimizedLLMRuntime<FP32>.generateUntilImEnd(
    tokenizer: Tokenizer,
    promptTokens: IntArray,
    maxTokens: Int,
    temperature: Float,
    onText: (String) -> Unit,
) {
    val stopMarker = "<|im_end|>"
    val buffer = StringBuilder()
    var stopped = false

    withContext(Dispatchers.Default) {
        generate(prompt = promptTokens, steps = maxTokens, temperature = temperature) { id ->
            if (stopped) return@generate
            buffer.append(tokenizer.decode(id))

            val idx = buffer.indexOf(stopMarker)
            if (idx >= 0) {
                if (idx > 0) onText(buffer.substring(0, idx))
                buffer.clear()
                stopped = true
                return@generate
            }

            // Hold back tail equal to stopMarker length so a partial match
            // doesn't leak into the UI before we confirm/deny it.
            val safe = (buffer.length - stopMarker.length).coerceAtLeast(0)
            if (safe > 0) {
                val emitted = buffer.substring(0, safe)
                onText(emitted)
                val tail = buffer.substring(safe)
                buffer.clear()
                buffer.append(tail)
            }
        }
    }

    if (!stopped && buffer.isNotEmpty()) onText(buffer.toString())
}
