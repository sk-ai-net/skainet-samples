package sk.ainet.samples.glove

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CooccurrenceTest {

    @Test
    fun adjacentTokensGetWeightOne() {
        // ids: a=0 b=1 c=2
        val vocab = Vocabulary.build(tokenize("a b c"))
        val ids = vocab.encode(tokenize("a b c"))
        val cooc = buildCooccurrences(ids, windowSize = 2)

        // a<->b distance 1 -> weight 1.0
        assertEquals(1.0f, cooc.getValue(PairKey(vocab.idOf("a"), vocab.idOf("b"))))
        // a<->c distance 2 -> weight 0.5
        assertEquals(0.5f, cooc.getValue(PairKey(vocab.idOf("a"), vocab.idOf("c"))))
    }

    @Test
    fun cooccurrenceIsSymmetric() {
        val vocab = Vocabulary.build(tokenize("a b c"))
        val ids = vocab.encode(tokenize("a b c"))
        val cooc = buildCooccurrences(ids, windowSize = 2)

        for ((pair, count) in cooc) {
            assertEquals(count, cooc.getValue(PairKey(pair.context, pair.center)))
        }
    }

    @Test
    fun windowSizeLimitsReach() {
        val vocab = Vocabulary.build(tokenize("a b c d"))
        val ids = vocab.encode(tokenize("a b c d"))
        val cooc = buildCooccurrences(ids, windowSize = 1)

        // With window 1, a never reaches c.
        assertTrue(PairKey(vocab.idOf("a"), vocab.idOf("c")) !in cooc)
    }

    @Test
    fun sentencesDoNotCoOccurAcrossBoundaries() {
        // "x" ends the first sentence and starts the second; per-sentence windowing
        // must not pair it with itself or with neighbours from the other sentence.
        val vocab = Vocabulary.build(tokenize("a x x b"))
        val sentences = listOf(
            vocab.encode(tokenize("a x")),
            vocab.encode(tokenize("x b")),
        )
        val cooc = buildCooccurrencesOverSentences(sentences, windowSize = 2)

        assertTrue(PairKey(vocab.idOf("x"), vocab.idOf("x")) !in cooc) // no self-pair
        assertTrue(PairKey(vocab.idOf("a"), vocab.idOf("b")) !in cooc) // no cross-sentence pair
        assertEquals(1.0f, cooc.getValue(PairKey(vocab.idOf("a"), vocab.idOf("x"))))
    }
}
