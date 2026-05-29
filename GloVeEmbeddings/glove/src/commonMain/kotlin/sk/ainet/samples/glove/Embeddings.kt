package sk.ainet.samples.glove

import kotlin.math.sqrt

/**
 * A read-only word-embedding store: a [vocab] plus a flat [vectors] table of shape
 * `[vocab.size * dim]`, laid out row-major by word ID. It supports cosine similarity,
 * nearest-neighbour search, and vector "algebra" such as the classic analogy
 * `king - man + woman ≈ queen`.
 *
 * The geometry is identical whether the vectors were learned by [GloVePlain]
 * (see [GloVePlain.toEmbeddings]) or loaded from a pretrained file via [GloVeTextReader],
 * so the same lookups work in both cases.
 */
public class Embeddings(
    public val vocab: Vocabulary,
    public val vectors: FloatArray,
    public val dim: Int,
) {
    init {
        require(dim > 0) { "dim must be positive, was $dim" }
        require(vectors.size == vocab.size * dim) {
            "vectors.size=${vectors.size} must equal vocab.size=${vocab.size} * dim=$dim"
        }
    }

    /** The [dim]-length vector for [word], as a fresh copy. */
    public fun vectorOf(word: String): FloatArray = vectorOf(vocab.idOf(word))

    /** The [dim]-length vector for word [id], as a fresh copy. */
    public fun vectorOf(id: Int): FloatArray {
        val offset = id * dim
        return FloatArray(dim) { vectors[offset + it] }
    }

    /** Cosine similarity between two word IDs (reuses the flat-array helper, §8). */
    public fun cosineSimilarity(a: Int, b: Int): Float = cosineSimilarity(vectors, a, b, dim)

    /** The [topK] words most similar to [query], excluding [query] itself. §8 */
    public fun nearestWords(query: String, topK: Int = 5): List<Pair<String, Float>> =
        nearestToVector(vectorOf(query), topK, exclude = setOf(query))

    /**
     * The [topK] words whose row vectors are most cosine-similar to an arbitrary [vector],
     * skipping any word in [exclude]. This is what powers [analogy] queries.
     */
    public fun nearestToVector(
        vector: FloatArray,
        topK: Int = 5,
        exclude: Set<String> = emptySet(),
    ): List<Pair<String, Float>> {
        require(vector.size == dim) { "vector.size=${vector.size} must equal dim=$dim" }
        val excludedIds = exclude.mapNotNull { vocab.wordToId[it] }.toSet()
        return vocab.wordToId.entries
            .asSequence()
            .filter { it.value !in excludedIds }
            .map { (word, id) -> word to cosineToRow(vector, id) }
            .sortedByDescending { it.second }
            .take(topK)
            .toList()
    }

    /**
     * Vector algebra: the [topK] words nearest to `vectorOf(a) - vectorOf(b) + vectorOf(c)`.
     * For "king - man + woman ≈ queen" call `analogy("king", "man", "woman")`. The three
     * input words are excluded from the results so the answer is a genuinely new word.
     */
    public fun analogy(a: String, b: String, c: String, topK: Int = 5): List<Pair<String, Float>> {
        val va = vectorOf(a)
        val vb = vectorOf(b)
        val vc = vectorOf(c)
        val target = FloatArray(dim) { va[it] - vb[it] + vc[it] }
        return nearestToVector(target, topK, exclude = setOf(a, b, c))
    }

    /** Cosine similarity between an arbitrary [vector] and row [id] of the table. */
    private fun cosineToRow(vector: FloatArray, id: Int): Float {
        val offset = id * dim
        var dot = 0f
        var normRow = 0f
        var normVec = 0f
        for (k in 0 until dim) {
            val a = vector[k]
            val b = vectors[offset + k]
            dot += a * b
            normVec += a * a
            normRow += b * b
        }
        val denom = sqrt(normVec.toDouble()).toFloat() * sqrt(normRow.toDouble()).toFloat()
        return if (denom == 0f) 0f else dot / denom
    }
}
