package sk.ainet.samples.kernelrace.engine

import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.tokenizer.TokenizerFactory
import sk.ainet.context.ExecutionContext
import sk.ainet.io.JvmRandomAccessSource
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.DecoderGgufWeightLoader
import sk.ainet.models.llama.LlamaNetworkLoader
import sk.ainet.samples.kernelrace.model.ModelData

/** File-based random-access reads, same shape as the Android fast path but without the
 *  NEON JNI kernels — desktop has no ARM hardware to dispatch to. */
actual suspend fun buildLlamaComponents(ctx: ExecutionContext, model: ModelData): LlamaComponents {
    val path = (model as ModelData.FilePath).path
    val weights = DecoderGgufWeightLoader(
        randomAccessProvider = { JvmRandomAccessSource.open(path) },
        quantPolicy = QuantPolicy.NATIVE_OPTIMIZED,
        acceptedArchitectures = setOf("llama", "mistral"),
    ).loadToMapStreaming<FP32, Float>(ctx)
    val runtime = OptimizedLLMRuntime(
        model = LlamaNetworkLoader.fromWeights(weights),
        ctx = ctx,
        mode = OptimizedLLMMode.DIRECT,
        dtype = FP32::class,
        bos = weights.metadata.bosTokenId,
    )
    val tokenizer = JvmRandomAccessSource.open(path).use { TokenizerFactory.fromGgufSource(it) }
    return LlamaComponents(runtime, tokenizer)
}
