package sk.ainet.samples.vec2text.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.io.JvmRandomAccessSource
import sk.ainet.io.safetensors.SafeTensorsParametersLoader
import sk.ainet.io.tokenizer.SentencePieceTokenizer
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.tensor.plus
import sk.ainet.lang.tensor.times
import sk.ainet.lang.types.FP32
import sk.ainet.models.t5.GtrEmbedder
import sk.ainet.models.t5.T5Config
import sk.ainet.models.t5.T5Runtime
import sk.ainet.models.t5.loadT5Weights
import sk.ainet.models.vec2text.CorrectorModel
import sk.ainet.models.vec2text.InversionModel
import sk.ainet.models.vec2text.Vec2TextInverter
import sk.ainet.models.vec2text.Vec2TextWeightLoader
import java.io.File

/** One correction step surfaced to the UI. */
data class Step(val step: Int, val text: String, val cosine: Float)

/**
 * Loads the gtr-base checkpoints once and drives the vec2text inversion loop **step by step**
 * (using the public InversionModel / CorrectorModel APIs) so the UI can render each hypothesis
 * as it's produced — the full loop takes minutes on CPU.
 */
class Vec2TextEngine private constructor(
    private val embedder: GtrEmbedder<FP32>,
    private val inversion: InversionModel<FP32>,
    private val corrector: CorrectorModel<FP32>,
    private val sp: SentencePieceTokenizer,
    private val cfg: T5Config,
) {
    private fun encodeForEmbedder(text: String): IntArray {
        val ids = sp.encode(text).take(cfg.maxSeqLength - 1).toMutableList()
        ids.add(cfg.eosTokenId)
        return ids.toIntArray()
    }

    private fun decode(ids: IntArray): String =
        sp.decode(ids.filter { it != 0 && it != cfg.eosTokenId }.toIntArray())

    /** Embed [text] into its `[768]` GTR sentence embedding. */
    fun embed(text: String): Tensor<FP32, Float> = embedder.embed(encodeForEmbedder(text))

    /** Raw float view of an embedding, for visualization. */
    fun toFloats(v: Tensor<FP32, Float>): FloatArray = v.data.copyToFloatArray()

    /** Linear interpolation `(1-alpha)*a + alpha*b` of two embeddings. */
    fun interpolate(a: Tensor<FP32, Float>, b: Tensor<FP32, Float>, alpha: Float): Tensor<FP32, Float> =
        (a * (1f - alpha)) + (b * alpha)

    /**
     * Invert [target] into text over [steps] correction rounds, calling [onStep] after the
     * initial hypothesis (step 0) and each correction. Returns the best-cosine hypothesis.
     */
    fun invert(target: Tensor<FP32, Float>, steps: Int, onStep: (Step) -> Unit): Step {
        var hypIds = inversion.invert(target, maxLength = cfg.maxSeqLength)
        var hypText = decode(hypIds)
        var cos = cosineOf(target, hypText)
        var best = Step(0, hypText, cos)
        onStep(best)

        for (s in 1..steps) {
            val hypEmb = embed(hypText)
            hypIds = corrector.correct(target, hypEmb, hypIds, maxLength = cfg.maxSeqLength)
            hypText = decode(hypIds)
            cos = cosineOf(target, hypText)
            val step = Step(s, hypText, cos)
            onStep(step)
            if (cos > best.cosine) best = step
        }
        return best
    }

    private fun cosineOf(target: Tensor<FP32, Float>, text: String): Float =
        Vec2TextInverter.cosine(target, embed(text))

    companion object {
        val REQUIRED = listOf(
            "tokenizer.json", "gtr_encoder.safetensors", "inversion.safetensors", "corrector.safetensors",
        )

        /** Resolve the models dir: `$VEC2TEXT_MODELS_DIR`, else `../models`, else `models`. */
        fun modelsDir(): File {
            System.getenv("VEC2TEXT_MODELS_DIR")?.let { return File(it) }
            for (c in listOf("../models", "models")) {
                val f = File(c)
                if (File(f, "tokenizer.json").exists()) return f
            }
            return File("../models")
        }

        fun missing(dir: File): List<String> = REQUIRED.filterNot { File(dir, it).exists() }

        /** Blocking load of all three models (call off the UI thread). */
        suspend fun load(dir: File): Vec2TextEngine {
            val ctx = DirectCpuExecutionContext()
            val cfg = T5Config()
            fun loader(name: String) =
                SafeTensorsParametersLoader(sourceProvider = { JvmRandomAccessSource.open(File(dir, name).toString()) })

            val gtr = loadT5Weights(loader("gtr_encoder.safetensors"), ctx, FP32::class, cfg, "", withDecoder = false)
            val embedder = GtrEmbedder(T5Runtime(ctx, gtr, FP32::class))
            val inversion = InversionModel(ctx, Vec2TextWeightLoader.loadInversion(loader("inversion.safetensors"), ctx, FP32::class, cfg), FP32::class)
            val corrector = CorrectorModel(ctx, Vec2TextWeightLoader.loadCorrector(loader("corrector.safetensors"), ctx, FP32::class, cfg), FP32::class)
            val sp = SentencePieceTokenizer.fromTokenizerJson(
                Json.parseToJsonElement(File(dir, "tokenizer.json").readText()).jsonObject
            )
            return Vec2TextEngine(embedder, inversion, corrector, sp, cfg)
        }
    }
}
