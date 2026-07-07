package sk.ainet.app.samples.tinytransformer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenizerTest {

    @Test
    fun tokenizeLowercasesAndKeepsPunctuation() {
        assertEquals(
            listOf("der", "hund", "bellt", "laut", "!"),
            WordTokenizer.tokenize("Der Hund bellt laut!")
        )
    }

    @Test
    fun tokenizeHandlesUmlauts() {
        assertEquals(listOf("der", "hamster", "mag", "körner"), WordTokenizer.tokenize("Der Hamster mag Körner"))
    }

    @Test
    fun specialsOccupyFixedIds() {
        val vocab = WordTokenizer.buildVocab(DEFAULT_CORPUS, DEFAULT_MAX_VOCAB)
        assertEquals(Vocab.PAD_TOKEN, vocab.tokenOf(Vocab.PAD))
        assertEquals(Vocab.UNK_TOKEN, vocab.tokenOf(Vocab.UNK))
        assertEquals(Vocab.EOS_TOKEN, vocab.tokenOf(Vocab.EOS))
    }

    @Test
    fun vocabIsFrequencyRankedAndCapped() {
        val vocab = WordTokenizer.buildVocab(DEFAULT_CORPUS, 5)
        assertEquals(5, vocab.size)
        // "der" appears 6 times — most frequent, so first after the specials.
        assertEquals("der", vocab.tokenOf(3))
    }

    @Test
    fun unknownWordsEncodeToUnk() {
        val vocab = WordTokenizer.buildVocab(DEFAULT_CORPUS, DEFAULT_MAX_VOCAB)
        val ids = WordTokenizer.encode("Der Elefant", vocab)
        assertEquals(listOf(vocab.idOf("der"), Vocab.UNK, Vocab.EOS), ids)
    }

    @Test
    fun windowsShiftByOneAndPad() {
        val vocab = WordTokenizer.buildVocab(DEFAULT_CORPUS, DEFAULT_MAX_VOCAB)
        val windows = WordTokenizer.windows(listOf("Der Hund bellt laut"), vocab, contextLen = 8)
        // sentence = [der, hund, bellt, laut, <eos>] → 4 windows (starts 0..3)
        assertEquals(4, windows.size)

        val first = windows.first()
        assertEquals(8, first.inputIds.size)
        assertEquals(8, first.targetIds.size)
        // target is the input shifted left by one
        assertEquals(first.inputIds.drop(1).dropLast(1), first.targetIds.dropLast(2))
        // right-padded with <pad>
        assertEquals(Vocab.PAD, first.inputIds.last())
        assertTrue(first.inputIds.take(4).none { it == Vocab.PAD })
    }

    @Test
    fun everyTargetPositionFollowsItsInput() {
        val vocab = WordTokenizer.buildVocab(DEFAULT_CORPUS, DEFAULT_MAX_VOCAB)
        val sentence = WordTokenizer.encode("Der Fisch schwimmt ruhig", vocab)
        val windows = WordTokenizer.windows(listOf("Der Fisch schwimmt ruhig"), vocab, contextLen = 3)
        for ((start, window) in windows.withIndex()) {
            for (t in window.inputIds.indices) {
                val expectedTarget = sentence.getOrElse(start + t + 1) { Vocab.PAD }
                assertEquals(expectedTarget, window.targetIds[t])
            }
        }
    }
}
