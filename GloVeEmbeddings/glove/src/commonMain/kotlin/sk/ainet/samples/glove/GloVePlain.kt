package sk.ainet.samples.glove

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The transparent, plain-Kotlin GloVe implementation (steps 4–8 of `glove.md`).
 *
 * Everything is kept as flat [FloatArray] math so the learning loop is fully visible —
 * nothing is hidden inside a framework. This is the teaching path; the runnable
 * SKaiNET-tensor counterpart lives in [GloVeTensors].
 */
public class GloVePlain(
    public val vocab: Vocabulary,
    public val cooccurrences: Map<PairKey, Float>,
    public val embeddingDim: Int = 4,
    seed: Int = 42,
) {
    private val random = Random(seed)
    private val vocabSize = vocab.size

    // Two vector tables: center-word (W) and context-word (C) embeddings. §4
    public val wordVectors: FloatArray = FloatArray(vocabSize * embeddingDim) { randomSmall() }
    public val contextVectors: FloatArray = FloatArray(vocabSize * embeddingDim) { randomSmall() }
    public val wordBias: FloatArray = FloatArray(vocabSize)
    public val contextBias: FloatArray = FloatArray(vocabSize)

    private fun randomSmall(): Float = (random.nextFloat() - 0.5f) * 0.02f

    /**
     * The GloVe prediction for a pair: dot(word, context) + biases. §5–6.
     * Should approximate log(co-occurrence count).
     */
    public fun predict(i: Int, j: Int): Float {
        val wi = i * embeddingDim
        val cj = j * embeddingDim
        return dot(wordVectors, wi, contextVectors, cj, embeddingDim) + wordBias[i] + contextBias[j]
    }

    /** One stochastic gradient-descent update over a single observed pair. §7. */
    public fun trainOnePair(i: Int, j: Int, count: Float, learningRate: Float = 0.05f) {
        val wi = i * embeddingDim
        val cj = j * embeddingDim

        val prediction = predict(i, j)
        val target = ln(count.toDouble()).toFloat()
        val weight = gloveWeight(count)

        val error = prediction - target
        val grad = 2f * weight * error

        // Save old values because both vectors are updated.
        val oldWord = FloatArray(embeddingDim) { k -> wordVectors[wi + k] }
        val oldContext = FloatArray(embeddingDim) { k -> contextVectors[cj + k] }

        for (k in 0 until embeddingDim) {
            wordVectors[wi + k] -= learningRate * grad * oldContext[k]
            contextVectors[cj + k] -= learningRate * grad * oldWord[k]
        }

        wordBias[i] -= learningRate * grad
        contextBias[j] -= learningRate * grad
    }

    /** Repeated updates over all observed co-occurrences. Returns per-epoch total loss. §7. */
    public fun train(epochs: Int = 50, learningRate: Float = 0.05f): List<Float> {
        val losses = ArrayList<Float>(epochs)
        repeat(epochs) {
            var totalLoss = 0f
            for ((pair, count) in cooccurrences) {
                val prediction = predict(pair.center, pair.context)
                val weight = gloveWeight(count)
                totalLoss += gloveLossForPair(prediction, count, weight)
                trainOnePair(pair.center, pair.context, count, learningRate)
            }
            losses += totalLoss
        }
        return losses
    }

    /** Cosine similarity between two word vectors (by ID). §8. */
    public fun cosineSimilarity(a: Int, b: Int): Float =
        cosineSimilarity(wordVectors, a, b, embeddingDim)

    /**
     * Expose the learned center-word table as a read-only [Embeddings] so the same
     * similarity / nearest-neighbour / analogy lookups work on trained and pretrained
     * vectors alike. Backed by the live [wordVectors] array (call after [train]).
     */
    public fun toEmbeddings(): Embeddings = Embeddings(vocab, wordVectors, embeddingDim)

    /** The topK most similar words to [query], excluding [query] itself. §8. */
    public fun nearestWords(query: String, topK: Int = 5): List<Pair<String, Float>> {
        val queryId = vocab.idOf(query)
        return vocab.wordToId.entries
            .filter { it.key != query }
            .map { (word, id) -> word to cosineSimilarity(queryId, id) }
            .sortedByDescending { it.second }
            .take(topK)
    }
}

// --- Article-faithful free helper functions (§5, §6, §8) ---

/** Dot product over a slice of two flat float arrays. */
public fun dot(a: FloatArray, aOffset: Int, b: FloatArray, bOffset: Int, dim: Int): Float {
    var sum = 0f
    for (k in 0 until dim) {
        sum += a[aOffset + k] * b[bOffset + k]
    }
    return sum
}

/** GloVe weighting function f(x): down-weights rare and caps frequent pairs. §6. */
public fun gloveWeight(x: Float, xMax: Float = 100f, alpha: Float = 0.75f): Float =
    if (x < xMax) (x / xMax).toDouble().pow(alpha.toDouble()).toFloat() else 1f

/** Weighted squared error against log(count). §6. */
public fun gloveLossForPair(prediction: Float, count: Float, weight: Float): Float {
    val target = ln(count.toDouble()).toFloat()
    val error = prediction - target
    return weight * error * error
}

/** Cosine similarity between rows [a] and [b] of a flat embedding table. §8. */
public fun cosineSimilarity(vectors: FloatArray, a: Int, b: Int, dim: Int): Float {
    val ao = a * dim
    val bo = b * dim

    var dot = 0f
    var normA = 0f
    var normB = 0f

    for (k in 0 until dim) {
        val av = vectors[ao + k]
        val bv = vectors[bo + k]

        dot += av * bv
        normA += av * av
        normB += bv * bv
    }

    return dot / (sqrt(normA.toDouble()).toFloat() * sqrt(normB.toDouble()).toFloat())
}
