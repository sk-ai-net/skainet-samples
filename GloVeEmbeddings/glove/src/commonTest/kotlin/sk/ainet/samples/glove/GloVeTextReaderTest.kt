package sk.ainet.samples.glove

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GloVeTextReaderTest {

    private val sample = """
        man 0.0 0.0
        woman 0.0 1.0
        king 1.0 0.0
        queen 1.0 1.0
    """.trimIndent()

    @Test
    fun parsesDimAndVocab() {
        val e = GloVeTextReader.parse(sample)
        assertEquals(2, e.dim)
        assertEquals(4, e.vocab.size)
        assertEquals(floatArrayOf(1.0f, 0.0f).toList(), e.vectorOf("king").toList())
    }

    @Test
    fun roundTripsThroughAnalogy() {
        val e = GloVeTextReader.parse(sample)
        assertEquals("queen", e.analogy("king", "man", "woman", topK = 1).single().first)
    }

    @Test
    fun skipsBlankAndRaggedLines() {
        val text = "a 1.0 0.0\n\nb 0.0 1.0\nc 1.0\nd 0.0 0.0 0.0"
        val e = GloVeTextReader.parse(text)
        // dim is inferred as 2 from line "a"; "c" (1 value) and "d" (3 values) are dropped.
        assertEquals(2, e.dim)
        assertEquals(2, e.vocab.size)
        assertTrue("c" !in e.vocab.wordToId && "d" !in e.vocab.wordToId)
    }
}
