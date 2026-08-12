package sk.ainet.samples.kernelrace.engine

import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.tokenizer.TokenizerFactory
import sk.ainet.context.ExecutionContext
import sk.ainet.io.AndroidRandomAccessSource
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.DecoderGgufWeightLoader
import sk.ainet.models.llama.LlamaNetworkLoader
import sk.ainet.samples.kernelrace.model.ModelData

/**
 * The "five lines" fast path: random-access reads straight off the file,
 * `QuantPolicy.NATIVE_OPTIMIZED` keeps Q8_0 packed for the hand-written ARM NEON kernels
 * (skainet-backend-jni-cpu), which register themselves through the kernel SPI just by being
 * on the classpath.
 */
actual suspend fun buildLlamaComponents(ctx: ExecutionContext, model: ModelData): LlamaComponents {
    val path = (model as ModelData.FilePath).path
    val weights = DecoderGgufWeightLoader(
        randomAccessProvider = { AndroidRandomAccessSource.open(path) },
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
    val tokenizer = AndroidRandomAccessSource.open(path).use { TokenizerFactory.fromGgufSource(it) }
    return LlamaComponents(runtime, tokenizer)
}
