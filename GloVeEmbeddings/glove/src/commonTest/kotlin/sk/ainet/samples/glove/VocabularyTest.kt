package sk.ainet.samples.glove

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VocabularyTest {

    @Test
    fun tokenizeLowercasesAndSplits() {
        val tokens = tokenize("Kotlin   is\nGREAT")
        assertEquals(listOf("kotlin", "is", "great"), tokens)
    }

    @Test
    fun vocabIndexingIsBijective() {
        val tokens = tokenize("kotlin is great kotlin")
        val vocab = Vocabulary.build(tokens)

        // "kotlin" appears twice but is one entry.
        assertEquals(3, vocab.size)
        for ((word, id) in vocab.wordToId) {
            assertEquals(word, vocab.wordOf(id))
            assertEquals(id, vocab.idOf(word))
        }
    }

    @Test
    fun encodeMapsTokensToIds() {
        val tokens = tokenize("a b a")
        val vocab = Vocabulary.build(tokens)
        val ids = vocab.encode(tokens)

        assertEquals(3, ids.size)
        assertEquals(ids[0], ids[2]) // both "a"
        assertTrue(ids[1] != ids[0]) // "b" differs
    }
}
