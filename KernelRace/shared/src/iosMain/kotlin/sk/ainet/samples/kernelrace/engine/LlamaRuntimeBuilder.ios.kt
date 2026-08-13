package sk.ainet.samples.kernelrace.engine

import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.tokenizer.TokenizerFactory
import sk.ainet.context.ExecutionContext
import sk.ainet.io.PosixPreadRandomAccessSource
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.DecoderGgufWeightLoader
import sk.ainet.models.llama.LlamaNetworkLoader
import sk.ainet.samples.kernelrace.model.ModelData

/** Same file-based random-access shape as the JVM/Android actuals, backed by POSIX `pread(2)`
 *  (shared by macOS/iOS/Linux native in skainet-io-core's native64Main) instead of a JDK/ART
 *  file API. `QuantPolicy.NATIVE_OPTIMIZED` keeps Q8_0 packed for the Apple native-cinterop
 *  kernels — see kllama's registerPlatformBackends()/installNativeKernels() wiring, which
 *  installs the same provider this app relies on. */
actual suspend fun buildLlamaComponents(ctx: ExecutionContext, model: ModelData): LlamaComponents {
    val path = (model as ModelData.FilePath).path
    fun openSource() = checkNotNull(PosixPreadRandomAccessSource.open(path)) { "Cannot open GGUF at $path" }
    val weights = DecoderGgufWeightLoader(
        randomAccessProvider = { openSource() },
        quantPolicy = QuantPolicy.NATIVE_OPTIMIZED,
        acceptedArchitectures = setOf("llama", "mistral"), // SmolLM2 is llama-family
    ).loadToMapStreaming<FP32, Float>(ctx)
    val runtime = OptimizedLLMRuntime(
        model = LlamaNetworkLoader.fromWeights(weights),
        ctx = ctx,
        mode = OptimizedLLMMode.DIRECT,
        dtype = FP32::class,
        bos = weights.metadata.bosTokenId,
    )
    val tokenizer = openSource().use { TokenizerFactory.fromGgufSource(it) }
    return LlamaComponents(runtime, tokenizer)
}
