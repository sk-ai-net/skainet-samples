package sk.ainet.samples.glove

/** The article's sample corpus (`glove.md` §1). */
public val SAMPLE_CORPUS: String = """
    kotlin is great for ai
    kotlin tensors help build embeddings
    embeddings represent words as vectors
""".trimIndent()

/**
 * Runs the full GloVe pipeline end to end and returns a human-readable report:
 * raw text → tokens → vocabulary → co-occurrence → training → cosine similarity,
 * plus the SKaiNET-tensor representation/scoring.
 */
public fun runGloveDemo(
    corpus: String = SAMPLE_CORPUS,
    windowSize: Int = 2,
    epochs: Int = 50,
    embeddingDim: Int = 16,
): String {
    val sb = StringBuilder()

    // §1–2: tokens, vocabulary, co-occurrence counts. Each line is treated as an
    // independent sentence so windows do not cross line boundaries.
    val sentences = corpus.lines().map { tokenize(it) }.filter { it.isNotEmpty() }
    val vocab = Vocabulary.build(sentences.flatten())
    val cooc = buildCooccurrencesOverSentences(sentences.map { vocab.encode(it) }, windowSize)

    sb.appendLine("Vocabulary (${vocab.size} words):")
    sb.appendLine("  ${vocab.wordToId}")
    sb.appendLine()
    sb.appendLine("Top co-occurrences:")
    cooc.entries.sortedByDescending { it.value }.take(8).forEach { (pair, count) ->
        sb.appendLine("  ${vocab.wordOf(pair.center)} -> ${vocab.wordOf(pair.context)} = $count")
    }

    // §4–8: transparent plain-Kotlin training + nearest neighbours.
    val plain = GloVePlain(vocab, cooc, embeddingDim = embeddingDim)
    val losses = plain.train(epochs)
    sb.appendLine()
    sb.appendLine("Training (plain Kotlin): epochs=$epochs loss ${losses.first()} -> ${losses.last()}")
    sb.appendLine("nearestWords(\"kotlin\"): ${plain.nearestWords("kotlin", topK = 3)}")

    // §3–5: the same data and scoring expressed with real SKaiNET tensors.
    val tensors = GloVeTensors(vocab, cooc, embeddingDim = embeddingDim)
    sb.appendLine()
    sb.appendLine("SKaiNET co-occurrence tensor shape: ${tensors.cooccurrenceTensor.shape}")
    sb.appendLine("SKaiNET scores (W matMul Cᵀ) shape:  ${tensors.scores().shape}")

    return sb.toString()
}
