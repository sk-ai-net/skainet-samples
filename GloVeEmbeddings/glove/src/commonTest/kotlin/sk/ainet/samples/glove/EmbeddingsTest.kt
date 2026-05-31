package sk.ainet.samples.glove

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmbeddingsTest {

    // A hand-made 2-D space where the analogy resolves exactly:
    //   king - man + woman = (1,0) - (0,0) + (0,1) = (1,1) = queen
    private fun toy(): Embeddings {
        val words = listOf("man", "woman", "king", "queen", "apple")
        val rows = floatArrayOf(
            0f, 0f,   // man
            0f, 1f,   // woman
            1f, 0f,   // king
            1f, 1f,   // queen
            -1f, -1f, // apple (opposite direction)
        )
        return Embeddings(Vocabulary.build(words), rows, dim = 2)
    }

    @Test
    fun analogyKingManWomanIsQueen() {
        val result = toy().analogy("king", "man", "woman", topK = 1)
        assertEquals("queen", result.single().first)
    }

    @Test
    fun analogyExcludesItsInputWords() {
        val result = toy().analogy("king", "man", "woman", topK = 5).map { it.first }
        assertTrue(result.none { it == "king" || it == "man" || it == "woman" })
    }

    @Test
    fun nearestToVectorRanksByCosine() {
        val e = toy()
        // The exact queen vector should rank queen first, apple (opposite) last.
        val ranked = e.nearestToVector(floatArrayOf(1f, 1f), topK = 5).map { it.first }
        assertEquals("queen", ranked.first())
        assertEquals("apple", ranked.last())
    }

    @Test
    fun vectorOfReturnsACopy() {
        val e = toy()
        val v = e.vectorOf("king")
        v[0] = 99f
        assertEquals(1f, e.vectorOf("king")[0]) // store is untouched
    }
}
