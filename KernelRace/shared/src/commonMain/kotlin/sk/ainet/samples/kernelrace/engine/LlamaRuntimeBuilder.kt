package sk.ainet.samples.kernelrace.engine

import kotlinx.io.Buffer
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.llm.tokenizer.GGUFTokenizer
import sk.ainet.context.ExecutionContext
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.LlamaNetworkLoader
import sk.ainet.samples.kernelrace.model.ModelData

/** Runtime + tokenizer built from the same GGUF bytes — kept together since both come from one load. */
class LlamaComponents(val runtime: OptimizedLLMRuntime<FP32>, val tokenizer: Tokenizer)

/**
 * Platform-split loader:
 * - Android takes the "five lines" fast path: random-access reads straight off the file,
 *   `QuantPolicy.NATIVE_OPTIMIZED` keeps Q8_0 packed for the hand-written ARM NEON kernels.
 * - Desktop JVM takes the same file-based path (no NEON kernels to dispatch to on non-Android
 *   hardware, but still avoids materializing the whole file on-heap).
 * - wasmJs has no filesystem, so it falls back to [buildLlamaComponentsFallback]: a sequential
 *   in-memory `Buffer` + `QuantPolicy.DEQUANTIZE_TO_FP32`. Same correctness, much slower —
 *   the packed-quant kernels are JVM/Android-only.
 */
expect suspend fun buildLlamaComponents(ctx: ExecutionContext, model: ModelData): LlamaComponents

/** Multiplatform fallback used by wasmJs (and available to every platform for testing). */
suspend fun buildLlamaComponentsFallback(ctx: ExecutionContext, bytes: ByteArray): LlamaComponents {
    val tokenizer = GGUFTokenizer.fromSource(Buffer().apply { write(bytes) }, false)
    val sourceProvider = { Buffer().apply { write(bytes) } }
    val model = LlamaNetworkLoader
        .fromGguf(sourceProvider, QuantPolicy.DEQUANTIZE_TO_FP32, false)
        .load<FP32, Float>(ctx)
    val runtime = OptimizedLLMRuntime(
        model = model,
        ctx = ctx,
        mode = OptimizedLLMMode.DIRECT,
        dtype = FP32::class,
        bos = tokenizer.bosTokenId,
    )
    return LlamaComponents(runtime, tokenizer)
}
