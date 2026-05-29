package sk.ainet.samples.glove

/**
 * Step 1 of the GloVe pipeline: words become integer IDs.
 *
 * See `glove.md` §1. A vocabulary maps each unique word to an integer index.
 * The ID is only a lookup key — the embedding is the dense vector learned later.
 */

/** Lower-case and split on whitespace, dropping empty tokens. */
public fun tokenize(text: String): List<String> =
    text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

/** Bidirectional mapping between words and their integer IDs. */
public class Vocabulary private constructor(
    public val wordToId: Map<String, Int>,
    public val idToWord: Map<Int, String>,
) {
    public val size: Int get() = wordToId.size

    public fun idOf(word: String): Int = wordToId.getValue(word)

    public fun wordOf(id: Int): String = idToWord.getValue(id)

    /** Convert a token stream into the IDs used by every later stage. */
    public fun encode(tokens: List<String>): List<Int> = tokens.map { idOf(it) }

    public companion object {
        public fun build(tokens: List<String>): Vocabulary {
            val wordToId = tokens.distinct().withIndex().associate { it.value to it.index }
            val idToWord = wordToId.entries.associate { it.value to it.key }
            return Vocabulary(wordToId, idToWord)
        }
    }
}
