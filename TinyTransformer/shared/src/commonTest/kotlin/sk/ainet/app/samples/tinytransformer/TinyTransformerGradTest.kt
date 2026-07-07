package sk.ainet.app.samples.tinytransformer

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.Phase
import sk.ainet.lang.graph.DefaultGradientTape
import sk.ainet.lang.graph.DefaultGraphExecutionContext
import sk.ainet.lang.nn.loss.CrossEntropyLoss
import sk.ainet.lang.nn.optim.sgd
import sk.ainet.lang.nn.trainStep
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.types.FP32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Gradient smoke test: one train step on a hand-made window must produce a
 * finite loss and update every parameter (embedding lookup, attention chain,
 * and output projection all backprop).
 */
class TinyTransformerGradTest {

    @Test
    fun oneTrainStepUpdatesAllParameters() {
        val contextLen = 4
        val vocabSize = 6
        val baseCtx = DirectCpuExecutionContext()
        val trainCtx = DefaultGraphExecutionContext(
            baseOps = baseCtx.ops,
            phase = Phase.TRAIN,
            createTapeFactory = { _ -> DefaultGradientTape() }
        )
        val model = TinyTransformer(vocabSize, contextLen, dModel = 8, ctx = trainCtx)

        val before = model.trainableParameters().map { p ->
            val shape = p.moduleParameter.value.shape
            p.name to FloatArray(shape.volume) { i ->
                readFlat(p.moduleParameter.value, shape, i)
            }
        }

        // window: tokens [3, 4, 5, pad], next-token targets [4, 5, pad, pad]
        val x = trainCtx.fromFloatArray<FP32, Float>(
            Shape(contextLen), FP32::class, floatArrayOf(3f, 4f, 5f, 0f)
        )
        val targets = FloatArray(contextLen * vocabSize)
        targets[0 * vocabSize + 4] = 1f
        targets[1 * vocabSize + 5] = 1f
        val y = trainCtx.fromFloatArray<FP32, Float>(Shape(contextLen, vocabSize), FP32::class, targets)

        val optimizer = sgd(lr = 0.5).apply {
            model.trainableParameters().forEach { addParameter(it) }
        }
        val loss = trainStep(model, CrossEntropyLoss(), optimizer, trainCtx, x, y)

        val lossValue = loss.data.get() as Float
        assertTrue(lossValue.isFinite(), "loss must be finite, was $lossValue")
        assertTrue(lossValue > 0f, "cross-entropy on an untrained model must be positive")

        for ((name, oldValues) in before) {
            val param = model.trainableParameters().first { it.name == name }.moduleParameter.value
            val changed = (0 until oldValues.size).any { i ->
                readFlat(param, param.shape, i) != oldValues[i]
            }
            assertTrue(changed, "parameter $name did not change after one SGD step")
        }
    }

    @Test
    fun attentionIsCausalAndNormalized() {
        val contextLen = 4
        val baseCtx = DirectCpuExecutionContext()
        val evalCtx = DefaultGraphExecutionContext(
            baseOps = baseCtx.ops,
            phase = Phase.EVAL,
            createTapeFactory = { _ -> DefaultGradientTape() }
        )
        val model = TinyTransformer(vocabSize = 6, contextLen = contextLen, dModel = 8, ctx = evalCtx)
        val x = evalCtx.fromFloatArray<FP32, Float>(
            Shape(contextLen), FP32::class, floatArrayOf(3f, 4f, 5f, 2f)
        )
        model.forward(x, evalCtx)

        val a = model.lastAttention
        for (i in 0 until contextLen) {
            var rowSum = 0f
            for (j in 0 until contextLen) {
                val value = a[i * contextLen + j]
                if (j > i) {
                    assertTrue(value < 1e-6f, "future position ($i,$j) must be masked, was $value")
                }
                rowSum += value
            }
            assertEquals(1f, rowSum, 1e-3f, "attention row $i must sum to 1")
        }
    }

    private fun readFlat(
        tensor: sk.ainet.lang.tensor.Tensor<*, *>,
        shape: Shape,
        flatIndex: Int,
    ): Float {
        val dims = shape.dimensions
        return when (dims.size) {
            1 -> tensor.data.get(flatIndex) as Float
            2 -> tensor.data.get(flatIndex / dims[1], flatIndex % dims[1]) as Float
            else -> error("unexpected rank ${dims.size}")
        }
    }
}
