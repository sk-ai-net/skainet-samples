package sk.ainet.app.samples.tinytransformer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.Phase
import sk.ainet.lang.graph.DefaultGradientTape
import sk.ainet.lang.graph.DefaultGraphExecutionContext
import sk.ainet.lang.nn.dsl.training
import sk.ainet.lang.nn.loss.CrossEntropyLoss
import sk.ainet.lang.nn.optim.sgd
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.types.FP32

/**
 * Trains the [TinyTransformer] on the tokenized corpus with the SKaiNET
 * training DSL: `training { model / loss / optimizer }` + `runner.step()`
 * on a TRAIN-phase graph execution context with a gradient tape.
 *
 * Mirrors the original KI-ENNA page: cross-entropy on next-token prediction
 * (pad positions masked via zeroed one-hot rows), plain SGD, one window per
 * optimizer step.
 */
class TinyTransformerTrainer(
    val vocab: Vocab,
    val windows: List<TrainingWindow>,
    val contextLen: Int,
    dModel: Int = EMBEDDING_DIM,
    seed: Int = 42,
) {
    private val baseCtx = DirectCpuExecutionContext()
    private val trainCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.TRAIN,
        createTapeFactory = { _ -> DefaultGradientTape() }
    )
    private val evalCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.EVAL,
        createTapeFactory = { _ -> DefaultGradientTape() }
    )

    // Parameters must live on the training context so the tape sees them.
    val model = TinyTransformer(vocab.size, contextLen, dModel, trainCtx, seed)

    private class Encoded(
        val x: Tensor<FP32, Float>,
        val y: Tensor<FP32, Float>,
        val tokens: List<String>,
        val realTokenCount: Int,
    )

    private val dataset: List<Encoded> = windows.map { window ->
        Encoded(
            x = encodeInput(window.inputIds),
            y = oneHotTargets(window.targetIds),
            tokens = window.inputIds.map(vocab::tokenOf),
            realTokenCount = window.targetIds.count { it != Vocab.PAD },
        )
    }

    fun predictor(): Predictor = Predictor(model, vocab, contextLen, evalCtx)

    /** Token → current embedding vector (first [dims] components), specials excluded. */
    fun embeddingTable(dims: Int = 6): List<Pair<String, FloatArray>> =
        (Vocab.SPECIALS.size until vocab.size).map { id ->
            vocab.tokenOf(id) to model.embeddingRow(id, dims)
        }

    /**
     * Cold flow emitting once per epoch; collection is cancellable between
     * steps, which is how Stop works. The `yield()` calls keep the UI alive on
     * single-threaded targets (JS/Wasm in the browser).
     */
    fun train(epochs: Int, learningRate: Float): Flow<TrainingProgress> = flow {
        val runner = training<FP32, Float> {
            model { model }
            loss { CrossEntropyLoss() }
            optimizer {
                sgd(lr = learningRate.toDouble()).apply {
                    model.trainableParameters().forEach { addParameter(it) }
                }
            }
        }

        for (epoch in 1..epochs) {
            var lossSum = 0f
            dataset.forEachIndexed { index, sample ->
                val lossTensor = runner.step(trainCtx, sample.x, sample.y)
                val rawLoss = lossTensor.data.get() as Float
                // The MEAN reduction averages over all T positions including the
                // zeroed pad rows; rescale so the reported loss is per real token,
                // like the reference implementation.
                val realCount = maxOf(1, sample.realTokenCount)
                lossSum += rawLoss * contextLen / realCount
                if (index % 8 == 7) yield()
            }

            // Stable heatmap: always snapshot the first window (eval phase, no tape).
            dataset.firstOrNull()?.let { probe -> model.forward(probe.x, evalCtx) }
            val attention = dataset.firstOrNull()?.let { probe ->
                AttentionSnapshot(probe.tokens, model.lastAttention.copyOf(), contextLen)
            }

            emit(
                TrainingProgress(
                    epoch = epoch,
                    loss = if (dataset.isEmpty()) 0f else lossSum / dataset.size,
                    attention = attention,
                    isCompleted = epoch == epochs,
                )
            )
            yield()
        }
    }

    private fun encodeInput(inputIds: List<Int>): Tensor<FP32, Float> =
        trainCtx.fromFloatArray(
            Shape(contextLen), FP32::class,
            FloatArray(contextLen) { i -> inputIds[i].toFloat() }
        )

    /**
     * One-hot FP32 targets `[T, V]`. Pad positions get an all-zero row: with
     * soft targets the cross-entropy of a zero row is exactly 0, which is the
     * pad masking. (Int32 index targets would bypass the gradient tape.)
     *
     * Created on the training context: the loss multiplies `targets * logProbs`
     * with the targets tensor as receiver, so the targets' ops must be the
     * taped graph ops or the loss would detach from the gradient tape.
     */
    private fun oneHotTargets(targetIds: List<Int>): Tensor<FP32, Float> {
        val vocabSize = vocab.size
        val values = FloatArray(contextLen * vocabSize)
        for (t in 0 until contextLen) {
            val target = targetIds[t]
            if (target != Vocab.PAD) {
                values[t * vocabSize + target] = 1f
            }
        }
        return trainCtx.fromFloatArray(Shape(contextLen, vocabSize), FP32::class, values)
    }
}
