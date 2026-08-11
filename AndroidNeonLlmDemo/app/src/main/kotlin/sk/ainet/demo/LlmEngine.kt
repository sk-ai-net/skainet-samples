package sk.ainet.demo

import android.util.Log
import java.io.File
import sk.ainet.apps.kllama.agent.generateUntilStop
import sk.ainet.apps.llm.InferenceRuntime
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.llm.tokenizer.TokenizerFactory
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.io.AndroidRandomAccessSource
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.DecoderGgufWeightLoader
import sk.ainet.models.llama.LlamaNetworkLoader

private const val TAG = "SKAINET_DEMO"

/**
 * Loads a GGUF LLM and streams generated tokens — the whole integration.
 *
 * The five lines inside [load] + [generate] are the Scene 4 shot of the
 * demo video: GGUF in, tokens out, no Python, no C++ in the app build.
 * The hand-written ARM NEON kernels (skainet-backend-jni-cpu) register
 * themselves through the kernel SPI just by being on the classpath.
 */
class LlmEngine private constructor(
    private val runtime: InferenceRuntime<FP32>,
    private val tokenizer: Tokenizer,
) {

    fun generate(prompt: String, maxTokens: Int = 200, onToken: (String) -> Unit): String {
        runtime.reset()
        // SmolLM2-Instruct is a ChatML model — a raw prompt makes it emit
        // <|im_end|> immediately. Wrap in the ChatML envelope it was trained on.
        val templated = "<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
        val promptTokens = tokenizer.encode(templated)
        Log.i(TAG, "prompt tokens=${promptTokens.size} eos=${tokenizer.eosTokenId}")
        val result = runtime.generateUntilStop(
            prompt = promptTokens,
            maxTokens = maxTokens,
            eosTokenId = tokenizer.eosTokenId,
            temperature = 0.7f,
            onToken = { tokenId ->
                Log.i(TAG, "token=$tokenId '${tokenizer.decode(tokenId)}'")
                onToken(tokenizer.decode(tokenId))
            },
            decode = { tokenizer.decode(it) },
        )
        Log.i(TAG, "result text len=${result.text.length}: '${result.text.take(200)}'")
        return result.text
    }

    companion object {

        /** Call from Dispatchers.IO — streams weights, never materializes the file on the ART heap. */
        suspend fun load(gguf: File): LlmEngine {
            // --- the five lines -------------------------------------------------
            val ctx = DirectCpuExecutionContext.create()
            val weights = DecoderGgufWeightLoader(
                randomAccessProvider = { AndroidRandomAccessSource.open(gguf.path) },
                quantPolicy = QuantPolicy.NATIVE_OPTIMIZED,           // keep Q8_0 packed for the NEON kernels
                acceptedArchitectures = setOf("llama", "mistral"),    // SmolLM2 is llama-family
            ).loadToMapStreaming<FP32, Float>(ctx)
            val runtime = OptimizedLLMRuntime(
                model = LlamaNetworkLoader.fromWeights(weights),
                ctx = ctx,
                mode = OptimizedLLMMode.DIRECT,
                dtype = FP32::class,
                bos = weights.metadata.bosTokenId,
            )
            val tokenizer = AndroidRandomAccessSource.open(gguf.path)
                .use { TokenizerFactory.fromGgufSource(it) }
            // --------------------------------------------------------------------

            return LlmEngine(runtime, tokenizer)
        }
    }
}
