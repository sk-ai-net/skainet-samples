package sk.ainet.samples.kernelrace.engine

import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.llm.tokenizer.TokenizerFactory
import sk.ainet.context.ExecutionContext
import sk.ainet.io.RandomAccessSource
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.LlamaNetworkLoader
import sk.ainet.samples.kernelrace.model.ModelData

/** Runtime + tokenizer built from the same GGUF bytes — kept together since both come from one load. */
class LlamaComponents(val runtime: OptimizedLLMRuntime<FP32>, val tokenizer: Tokenizer)

/**
 * In-memory [RandomAccessSource] over a fully-materialized ByteArray. wasm has no filesystem, so
 * the whole GGUF is already resident in memory anyway — this just satisfies the random-access-read
 * API the *streaming* GGUF loader wants, routing wasm through the same
 * `streamingTensorToTensor`/`loadToMapStreaming` path Android/JVM use, instead of SKaiNET's other,
 * independently-implemented sequential `readerTensorToTensor`/`loadToMap` path.
 *
 * That distinction matters, and isn't just style: the sequential path's `NATIVE_OPTIMIZED` handling
 * has a confirmed shape bug (it sizes the raw-bytes tensor by the logical element count instead of
 * `Shape(bytes.size)`, throwing "Data size doesn't match shape volume" for this model's tied
 * token_embd/output tensor — confirmed with a standalone Node/Kotlin-Wasm harness). Routing through
 * the streaming reader instead reuses the exact path already proven correct in production on
 * Android/JVM; verified in that same harness to produce byte-identical generated output to the
 * sequential+FP32 path this used to run, while using a fraction of the memory.
 */
private class InMemoryRandomAccessSource(private val bytes: ByteArray) : RandomAccessSource {
    override val size: Long = bytes.size.toLong()

    override fun readAt(position: Long, length: Int): ByteArray =
        bytes.copyOfRange(position.toInt(), (position + length).toInt())

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        val n = minOf(length, (size - position).toInt())
        bytes.copyInto(buffer, offset, position.toInt(), position.toInt() + n)
        return n
    }

    override fun close() {}
}

/**
 * Platform-split loader:
 * - Android takes the "five lines" fast path: random-access reads straight off the file,
 *   `QuantPolicy.NATIVE_OPTIMIZED` keeps Q8_0 packed for the hand-written ARM NEON kernels.
 * - Desktop JVM takes the same file-based path (no NEON kernels to dispatch to on non-Android
 *   hardware, but still avoids materializing the whole file on-heap).
 * - wasmJs has no filesystem, so it falls back to [buildLlamaComponentsFallback]: the whole GGUF
 *   is read into memory once, then wrapped in [InMemoryRandomAccessSource] so it can go through
 *   the same streaming loader + `NATIVE_OPTIMIZED` path Android/JVM use (see that class's doc —
 *   this fixes a real correctness bug in the alternative path, not just memory use). As of
 *   SKaiNET 0.40.x, `skainet-backend-cpu-wasm-js` registers `ScalarKernelProvider` (a portable,
 *   non-SIMD Kotlin matmul — same source as every other platform's scalar fallback) which
 *   implements `matmulQ8_0`/`matmulQ4_K`/etc., so `NATIVE_OPTIMIZED` works here too: quantized
 *   weights stay packed instead of being dequantized to FP32 (4x the footprint).
 *
 *   The tokenizer gets the same treatment for the same reason: `GGUFTokenizer.fromSource` (the
 *   old choice here) and `TokenizerFactory.fromGgufSource` (what `DesktopModelProvider`/
 *   `AndroidModelProvider` actually use) are two independently-implemented BPE tokenizers, not
 *   two code paths in the same one — confirmed to disagree on this exact model (16 vs 25 tokens
 *   for an identical prompt string), which is what was actually causing "hallucinated" answers,
 *   not model quality or numeric precision. `TokenizerFactory.fromGgufSource` + the same
 *   in-memory random-access source is verified byte-for-byte identical to the real JVM output.
 */
expect suspend fun buildLlamaComponents(ctx: ExecutionContext, model: ModelData): LlamaComponents

/** Multiplatform fallback used by wasmJs (and available to every platform for testing). */
suspend fun buildLlamaComponentsFallback(ctx: ExecutionContext, bytes: ByteArray): LlamaComponents {
    val tokenizer = TokenizerFactory.fromGgufSource(InMemoryRandomAccessSource(bytes))
    val randomAccessProvider = { InMemoryRandomAccessSource(bytes) }
    val model = LlamaNetworkLoader
        .fromGguf(randomAccessProvider, QuantPolicy.NATIVE_OPTIMIZED, false)
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
