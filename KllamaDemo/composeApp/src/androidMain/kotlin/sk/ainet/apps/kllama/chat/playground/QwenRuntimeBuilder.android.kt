package sk.ainet.apps.kllama.chat.playground

import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32

// Android API 35 still doesn't ship java.lang.foreign.Arena, so the JVM
// fast path (DecoderGgufMemSegConverter) isn't available. Fall back to
// the multiplatform Buffer-based loader.
internal actual suspend fun buildQwenRuntime(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> = buildQwenRuntimeFallback(ctx, bytes, bosTokenId)
