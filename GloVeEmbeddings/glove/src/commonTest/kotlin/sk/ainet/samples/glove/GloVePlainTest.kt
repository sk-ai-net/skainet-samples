package sk.ainet.samples.glove

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GloVePlainTest {

    private fun model(epochsReady: Boolean = false): GloVePlain {
        val tokens = tokenize(SAMPLE_CORPUS)
        val vocab = Vocabulary.build(tokens)
        val cooc = buildCooccurrences(vocab.encode(tokens), windowSize = 2)
        return GloVePlain(vocab, cooc, embeddingDim = 16).also {
            if (epochsReady) it.train(50)
        }
    }

    @Test
    fun trainingReducesLoss() {
        val losses = model().train(50)
        assertTrue(
            losses.last() < losses.first(),
            "loss should decrease: first=${losses.first()} last=${losses.last()}",
        )
    }

    @Test
    fun cosineSimilarityWithSelfIsOne() {
        val m = model(epochsReady = true)
        val id = m.vocab.idOf("kotlin")
        assertEquals(1.0f, m.cosineSimilarity(id, id), 1e-3f)
    }

    @Test
    fun nearestWordsReturnsTopKExcludingQuery() {
        val m = model(epochsReady = true)
        val neighbours = m.nearestWords("kotlin", topK = 3)

        assertEquals(3, neighbours.size)
        assertTrue(neighbours.none { it.first == "kotlin" })
        // Results are sorted by descending similarity.
        val sims = neighbours.map { it.second }
        assertEquals(sims.sortedDescending(), sims)
    }
}
