package sk.ainet.app.samples.tinytransformer

import sk.ainet.context.ExecutionContext
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.types.FP32
import kotlin.math.exp

/**
 * Next-word prediction with the (partially) trained model.
 *
 * The prompt is tokenized and laid out exactly like a training window
 * (right-padded to the context length), and the distribution is read at the
 * last real token's position — the position the model was supervised on.
 */
class Predictor(
    private val model: TinyTransformer,
    private val vocab: Vocab,
    private val contextLen: Int,
    private val ctx: ExecutionContext,
) {
    data class Prediction(
        /** Top-k `(token, probability)`, specials excluded, best first. */
        val topK: List<Pair<String, Float>>,
        val attention: AttentionSnapshot,
    )

    fun predictNext(prompt: String, k: Int = 5): Prediction? {
        val tokens = WordTokenizer.tokenize(prompt)
        if (tokens.isEmpty()) return null

        val ids = tokens.map(vocab::idOf).takeLast(contextLen)
        val window = List(contextLen) { i -> ids.getOrElse(i) { Vocab.PAD } }
        val lastRealPosition = ids.size - 1

        val x = ctx.fromFloatArray<FP32, Float>(
            Shape(contextLen), FP32::class,
            FloatArray(contextLen) { i -> window[i].toFloat() }
        )
        val logits = model.forward(x, ctx)

        // Softmax over the vocabulary at the last real position, computed in
        // plain Kotlin — no gradients or tensor ops needed for readout.
        val row = FloatArray(vocab.size) { v ->
            logits.data.get(lastRealPosition, v) as Float
        }
        val max = row.max()
        val expRow = FloatArray(vocab.size) { v -> exp(row[v] - max) }
        val sum = expRow.sum()

        val topK = (0 until vocab.size)
            .filterNot(vocab::isSpecial)
            .map { id -> vocab.tokenOf(id) to expRow[id] / sum }
            .sortedByDescending { it.second }
            .take(k)

        val attention = AttentionSnapshot(
            tokens = window.map(vocab::tokenOf),
            matrix = model.lastAttention.copyOf(),
            size = contextLen,
        )
        return Prediction(topK, attention)
    }
}
