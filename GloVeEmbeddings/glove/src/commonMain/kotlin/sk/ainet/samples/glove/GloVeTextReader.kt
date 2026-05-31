package sk.ainet.samples.glove

/**
 * Parses pretrained word vectors in the standard GloVe text format — one word per line,
 * the word followed by its floating-point components:
 *
 * ```
 * the 0.418 0.24968 -0.41242 ...
 * king 0.50451 0.68607 -0.59517 ...
 * ```
 *
 * The dimension is inferred from the first parsed line; lines whose arity differs (or that
 * carry an unparseable number) are skipped. Reading the bytes — an Android asset, a Compose
 * resource, a file — is the caller's job; pass the decoded text (or its lines). Keeping I/O
 * out of this module means it has no extra dependency and stays multiplatform.
 */
public object GloVeTextReader {

    private val WHITESPACE = Regex("\\s+")

    /** Parse the whole [text] (newline-separated) into an [Embeddings]. */
    public fun parse(text: String): Embeddings = parse(text.lineSequence())

    /** Parse [lines] in GloVe text format into an [Embeddings]. */
    public fun parse(lines: Sequence<String>): Embeddings {
        val words = ArrayList<String>()
        val flat = ArrayList<Float>()
        val seen = HashSet<String>()
        var dim = -1

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            val parts = line.split(WHITESPACE)
            if (parts.size < 2) continue

            val word = parts[0]
            val componentCount = parts.size - 1
            if (dim == -1) dim = componentCount
            if (componentCount != dim) continue        // malformed / ragged line
            if (!seen.add(word)) continue              // keep vocab and rows aligned

            val parsed = FloatArray(dim)
            var ok = true
            for (k in 0 until dim) {
                val v = parts[k + 1].toFloatOrNull()
                if (v == null) { ok = false; break }
                parsed[k] = v
            }
            if (!ok) { seen.remove(word); continue }

            words.add(word)
            for (v in parsed) flat.add(v)
        }

        require(dim > 0 && words.isNotEmpty()) { "no GloVe vectors could be parsed" }
        return Embeddings(Vocabulary.build(words), flat.toFloatArray(), dim)
    }
}
