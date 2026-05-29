package sk.ainet.app.samples.sinus

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.ExecutionContext
import sk.ainet.context.data
import sk.ainet.context.observers.LatencyExecutionObserver
import sk.ainet.execute.context.computation
import sk.ainet.lang.model.dnn.mlp.pretrained.SinusApproximatorWandB
import sk.ainet.lang.nn.definition
import sk.ainet.lang.nn.network
import sk.ainet.lang.tensor.dsl.tensor
import sk.ainet.lang.tensor.relu
import sk.ainet.lang.types.FP32


class SineNN(private val ctx: ExecutionContext) {

    val sinusApproximatorWandB = SinusApproximatorWandB()


    fun createModel(context: ExecutionContext) = definition<FP32, Float> {
        network(context) {
            input(1, "input")  // Single input for x value

            // First hidden layer: 1 -> 16 neurons
            dense(16, "hidden-1") {
                // Weights: 16x1 matrix - explicitly defined values
                weights {
                    fromArray(
                        sinusApproximatorWandB.getLayer1WandB("").weights
                    )
                }
                // Bias: 16 values - explicitly defined
                bias {
                    fromArray(
                        sinusApproximatorWandB.getLayer1WandB("").bias
                    )
                }
                activation = { it.relu() }
            }

            // Second hidden layer: 16 -> 16 neurons
            dense(16, "hidden-2") {
                // Weights: 16x16 matrix - explicitly defined values
                weights {
                    fromArray(
                        sinusApproximatorWandB.getLayer2WandB("").weights
                    )
                }
                // Bias: 16 values - explicitly defined
                bias {
                    fromArray(
                        sinusApproximatorWandB.getLayer2WandB("").bias
                    )
                }
                activation = { it.relu() }
            }

            // Output layer: 16 -> 1 neuron
            dense(1, "output") {
                // Weights: 1x16 matrix - explicitly defined values
                weights {
                    fromArray(
                        sinusApproximatorWandB.getLayer3WandB("").weights
                    )
                }

                // Bias: single value - explicitly defined
                bias {
                    fromArray(
                        sinusApproximatorWandB.getLayer3WandB("").bias
                    )
                }
            }
        }
    }

    val model = createModel(ctx)

    fun calcSine(angle: Float): Float =
        computation<Float>(ctx) { computation ->
            // Create a simple input tensor compatible with the model's expected input size (1)
            val inputTensor = data<FP32, Float>(ctx) {
                tensor<FP32, Float>() {
                    // Using shape(1, 1) to represent a single scalar input in 2D form
                    shape(1, 1) {
                        fromArray(
                            floatArrayOf(angle)
                        )
                    }
                }
            }

            model.forward(inputTensor, ctx).data[0, 0]
        }
}

class MLPSinusCalculator() : SinusCalculator {
    private val ctx = DirectCpuExecutionContext()
    private val observer = LatencyExecutionObserver()

    init {
        ctx.registerObserver(observer)
    }

    val _model = SineNN(ctx)
    val model = _model.model


    override fun calculate(angle: Float): Float = _model.calcSine(angle)

    fun getLatencyResults() = observer.results()

    override suspend fun loadModel() {
        // TODO model has pretrained weights as a part of model
    }
}

class TrainedSinusCalculator(val model: sk.ainet.lang.nn.Module<FP32, Float>) : SinusCalculator {
    private val ctx = DirectCpuExecutionContext()

    fun sk.ainet.lang.nn.Module<FP32, Float>.calcSine(ctx: ExecutionContext, angle: Float): Float {
        val model_: sk.ainet.lang.nn.Module<FP32, kotlin.Float> = this
        return computation<Float>(ctx) {
            model_.forward(
                data<FP32, Float>(ctx) {
                    tensor<FP32, Float>() {
                        shape(1, 1) {
                            fromArray(floatArrayOf(angle))
                        }
                    }
                }, sk.ainet.lang.graph.DefaultGraphExecutionContext(baseOps = ctx.ops, phase = sk.ainet.context.Phase.EVAL)
            ).data[0, 0]
        }
    }

    override fun calculate(angle: Float): Float = model.calcSine(ctx, angle)

    override suspend fun loadModel() {
    }
}

class PretrainedSinusCalculator() : SinusCalculator {
    private val ctx = DirectCpuExecutionContext()
    private val _model = SineNN(ctx)

    override fun calculate(angle: Float): Float = _model.calcSine(angle)

    override suspend fun loadModel() {
    }
}


