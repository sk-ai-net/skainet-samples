package sk.ainet.app.samples.tinytransformer

/**
 * Word-level tokenizer with a frequency-capped vocabulary, mirroring the
 * tokenizer of the original KI-ENNA page: lowercase, split on whitespace,
 * punctuation kept as separate tokens, specials `<pad>`, `<unk>`, `<eos>`.
 */
data class Vocab(
    val idToToken: List<String>,
    val tokenToId: Map<String, Int>,
) {
    val size: Int get() = idToToken.size

    /** Vocabulary size without the three special tokens (the KPI shown in the UI). */
    val sizeWithoutSpecials: Int get() = size - SPECIALS.size

    fun idOf(token: String): Int = tokenToId[token] ?: UNK

    fun tokenOf(id: Int): String = idToToken.getOrElse(id) { UNK_TOKEN }

    fun isSpecial(id: Int): Boolean = id < SPECIALS.size

    companion object {
        const val PAD = 0
        const val UNK = 1
        const val EOS = 2
        const val PAD_TOKEN = "<pad>"
        const val UNK_TOKEN = "<unk>"
        const val EOS_TOKEN = "<eos>"
        val SPECIALS = listOf(PAD_TOKEN, UNK_TOKEN, EOS_TOKEN)
    }
}

/** One sliding training window: `targetIds[t]` is the next token after `inputIds[t]`. */
class TrainingWindow(
    val inputIds: List<Int>,
    val targetIds: List<Int>,
)

object WordTokenizer {

    private val PUNCTUATION = ".,;:!?()[]{}\"'“”„-".toSet()

    /** Lowercase word tokens; punctuation characters become tokens of their own. */
    fun tokenize(text: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotEmpty()) {
                out += current.toString().lowercase()
                current.clear()
            }
        }
        for (ch in text) {
            when {
                ch.isWhitespace() -> flush()
                ch in PUNCTUATION -> {
                    flush()
                    out += ch.toString()
                }
                else -> current.append(ch)
            }
        }
        flush()
        return out
    }

    /**
     * Builds the vocabulary: specials first, then corpus tokens by descending
     * frequency (ties broken alphabetically, like the reference), capped at [maxVocab].
     */
    fun buildVocab(corpusLines: List<String>, maxVocab: Int): Vocab {
        val frequency = mutableMapOf<String, Int>()
        for (line in corpusLines) {
            for (token in tokenize(line)) {
                frequency[token] = (frequency[token] ?: 0) + 1
            }
        }
        val ranked = frequency.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
            .filter { it !in Vocab.SPECIALS }

        val idToToken = (Vocab.SPECIALS + ranked).take(maxOf(maxVocab, Vocab.SPECIALS.size))
        val tokenToId = idToToken.withIndex().associate { (i, t) -> t to i }
        return Vocab(idToToken, tokenToId)
    }

    /** Token ids of one sentence with `<eos>` appended. */
    fun encode(line: String, vocab: Vocab): List<Int> {
        val ids = tokenize(line).map { vocab.idOf(it) }
        return if (ids.isEmpty()) ids else ids + Vocab.EOS
    }

    /**
     * Sliding next-token windows of length [contextLen] over every sentence,
     * right-padded with `<pad>` — exactly the reference's `make_windows`.
     */
    fun windows(corpusLines: List<String>, vocab: Vocab, contextLen: Int): List<TrainingWindow> {
        val result = mutableListOf<TrainingWindow>()
        for (line in corpusLines) {
            val sentence = encode(line, vocab)
            if (sentence.size < 2) continue
            for (start in 0 until maxOf(1, sentence.size - 1)) {
                val input = padded(sentence, start, contextLen)
                val target = padded(sentence, start + 1, contextLen)
                result += TrainingWindow(input, target)
            }
        }
        return result
    }

    private fun padded(sentence: List<Int>, start: Int, length: Int): List<Int> =
        List(length) { i -> sentence.getOrElse(start + i) { Vocab.PAD } }
}
