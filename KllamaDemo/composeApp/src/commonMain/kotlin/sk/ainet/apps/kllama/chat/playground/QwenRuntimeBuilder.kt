package sk.ainet.apps.kllama.chat.playground

import kotlinx.io.Buffer
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.qwen.QwenNetworkLoader

/**
 * Platform-split loader. On JVM we take the fast path:
 * `JvmRandomAccessSource` over a temp file → `QuantPolicy.NATIVE_OPTIMIZED`
 * → `DecoderGgufMemSegConverter` wraps Q4_0 / Q8_0 tensors as
 * `Q4MemorySegmentTensorData` / `Q8MemorySegmentTensorData`. SIMD packed-
 * byte matmul kernels dispatch on those markers at forward time, no
 * per-call dequant.
 *
 * On wasmJs / iOS / Android we fall back to [buildQwenRuntimeFallback]:
 * a sequential `kotlinx.io.Source` over the byte buffer +
 * `QuantPolicy.DEQUANTIZE_TO_FP32`. Same correctness, much slower
 * throughput — the SIMD/MemSeg kernels are JVM-only.
 */
internal expect suspend fun buildQwenRuntime(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32>

/**
 * Slow but multiplatform fallback used by wasmJs / iOS / Android.
 * Dequantizes Q4_0 weights to FP32 during load (~1.5 GB working set for
 * Qwen3-0.6B). Forward pass is then plain FP32 matmul, no SIMD packed
 * kernels — perceptibly slow on a CPU. The JVM `actual` skips this and
 * uses the packed-quant path; everywhere else this is the best we can do
 * until upstream lands a wasmJs/Native SIMD backend.
 */
internal suspend fun buildQwenRuntimeFallback(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> {
    val sourceProvider = { Buffer().apply { write(bytes) } }
    val model = QwenNetworkLoader
        .fromGguf(sourceProvider, QuantPolicy.DEQUANTIZE_TO_FP32)
        .load<FP32, Float>(ctx)

    return OptimizedLLMRuntime(
        model = model,
        ctx = ctx,
        mode = OptimizedLLMMode.DIRECT,
        dtype = FP32::class,
        bos = bosTokenId,
    )
}
