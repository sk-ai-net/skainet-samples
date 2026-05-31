package sk.ainet.samples.glove

import sk.ainet.lang.tensor.Shape
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tensor-path tests run on the JVM, where the CPU backend is fully exercised.
 */
class GloVeTensorsTest {

    private fun model(): GloVeTensors {
        val tokens = tokenize(SAMPLE_CORPUS)
        val vocab = Vocabulary.build(tokens)
        val cooc = buildCooccurrences(vocab.encode(tokens), windowSize = 2)
        return GloVeTensors(vocab, cooc, embeddingDim = 8)
    }

    @Test
    fun cooccurrenceTensorHasVocabSquaredShape() {
        val m = model()
        val v = m.vocab.size
        assertEquals(Shape(v, v), m.cooccurrenceTensor.shape)
    }

    @Test
    fun scoresAreVocabByVocab() {
        val m = model()
        val v = m.vocab.size
        assertEquals(Shape(v, v), m.scores().shape)
    }
}
