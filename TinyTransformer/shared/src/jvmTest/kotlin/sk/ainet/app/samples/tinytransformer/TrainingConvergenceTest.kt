package sk.ainet.app.samples.tinytransformer

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end proof that training learns the default corpus: the loss must
 * drop substantially and the model must reproduce the corpus continuations.
 */
class TrainingConvergenceTest {

    @Test
    fun lossDropsAndModelLearnsTheCorpus() = runTest {
        val vocab = WordTokenizer.buildVocab(DEFAULT_CORPUS, DEFAULT_MAX_VOCAB)
        val windows = WordTokenizer.windows(DEFAULT_CORPUS, vocab, DEFAULT_CONTEXT_LEN)
        val trainer = TinyTransformerTrainer(vocab, windows, DEFAULT_CONTEXT_LEN)

        val losses = mutableListOf<Float>()
        trainer.train(epochs = 200, learningRate = DEFAULT_LEARNING_RATE).collect { progress ->
            losses += progress.loss
        }

        val first = losses.first()
        val last = losses.last()
        assertTrue(last < first * 0.5f, "loss should at least halve: first=$first last=$last")
        assertTrue(last < 1.5f, "final loss should be low, was $last")

        val predictor = trainer.predictor()

        val afterHund = assertNotNull(predictor.predictNext("Der Hund"))
        val hundTop2 = afterHund.topK.take(2).map { it.first }
        assertTrue(
            hundTop2.any { it == "bellt" || it == "frisst" },
            "'der hund' should continue with bellt/frisst, got $hundTop2"
        )

        val afterFisch = assertNotNull(predictor.predictNext("Der Fisch"))
        val fischTop2 = afterFisch.topK.take(2).map { it.first }
        assertTrue(
            fischTop2.any { it == "schwimmt" || it == "knabbert" },
            "'der fisch' should continue with schwimmt/knabbert, got $fischTop2"
        )
    }
}
