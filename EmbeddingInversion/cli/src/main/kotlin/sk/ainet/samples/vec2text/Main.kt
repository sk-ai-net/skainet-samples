package sk.ainet.samples.vec2text

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.io.JvmRandomAccessSource
import sk.ainet.io.safetensors.SafeTensorsParametersLoader
import sk.ainet.io.tokenizer.SentencePieceTokenizer
import sk.ainet.models.t5.GtrEmbedder
import sk.ainet.models.t5.T5Config
import sk.ainet.models.t5.T5Runtime
import sk.ainet.models.t5.loadT5Weights
import sk.ainet.models.vec2text.CorrectorModel
import sk.ainet.models.vec2text.InversionModel
import sk.ainet.models.vec2text.Vec2TextInverter
import sk.ainet.models.vec2text.Vec2TextTokenizer
import sk.ainet.models.vec2text.Vec2TextWeightLoader
import sk.ainet.lang.types.FP32
import java.io.File

/**
 * EmbeddingInversion CLI — decode a sentence embedding back into text with vec2text on SKaiNET.
 *
 * Usage:
 *   ./gradlew :cli:run --args="<text to invert>"
 * Model directory (default ./models) can be overridden with VEC2TEXT_MODELS_DIR.
 * Needs gtr_encoder / inversion / corrector .safetensors + tokenizer.json (see README).
 */
fun main(args: Array<String>) = runBlocking {
    val modelsDir = File(System.getenv("VEC2TEXT_MODELS_DIR") ?: "models")
    val text = args.joinToString(" ").ifBlank {
        "jack morris is a phd student at cornell tech in new york city"
    }
    val steps = System.getenv("VEC2TEXT_STEPS")?.toIntOrNull() ?: 5

    val required = listOf("tokenizer.json", "gtr_encoder.safetensors", "inversion.safetensors", "corrector.safetensors")
    val missing = required.filterNot { File(modelsDir, it).exists() }
    if (missing.isNotEmpty()) {
        System.err.println("Missing model files in ${modelsDir.absolutePath}: $missing")
        System.err.println("See README.md for how to produce them, or set VEC2TEXT_MODELS_DIR.")
        return@runBlocking
    }

    val ctx = DirectCpuExecutionContext()
    val cfg = T5Config()
    fun loader(name: String) =
        SafeTensorsParametersLoader(sourceProvider = { JvmRandomAccessSource.open(File(modelsDir, name).toString()) })

    println("Loading models from ${modelsDir.absolutePath} …")
    val gtr = loadT5Weights(loader("gtr_encoder.safetensors"), ctx, FP32::class, cfg, "", withDecoder = false)
    val embedder = GtrEmbedder(T5Runtime(ctx, gtr, FP32::class))
    val inversion = InversionModel(ctx, Vec2TextWeightLoader.loadInversion(loader("inversion.safetensors"), ctx, FP32::class, cfg), FP32::class)
    val corrector = CorrectorModel(ctx, Vec2TextWeightLoader.loadCorrector(loader("corrector.safetensors"), ctx, FP32::class, cfg), FP32::class)

    val sp = SentencePieceTokenizer.fromTokenizerJson(
        Json.parseToJsonElement(File(modelsDir, "tokenizer.json").readText()).jsonObject
    )
    val codec = object : Vec2TextTokenizer {
        override fun encodeForEmbedder(text: String): IntArray {
            val ids = sp.encode(text).take(cfg.maxSeqLength - 1).toMutableList()
            ids.add(cfg.eosTokenId)
            return ids.toIntArray()
        }
        override fun decode(ids: IntArray): String =
            sp.decode(ids.filter { it != 0 && it != cfg.eosTokenId }.toIntArray())
    }

    println("Inverting (≤$steps correction steps, greedy)…\n")
    val result = Vec2TextInverter(embedder, inversion, corrector, codec)
        .invert(text, numSteps = steps, maxLength = cfg.maxSeqLength)

    println("original:      $text")
    println("reconstructed: ${result.text}")
    println("cosine:        ${"%.4f".format(result.cosine)}\n")
    println("trace:")
    result.trace.forEach { println("  step ${it.step}: cos=${"%.4f".format(it.cosine)}  \"${it.text}\"") }
}
