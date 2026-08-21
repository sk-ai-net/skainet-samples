package org.example

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.Phase
import sk.ainet.lang.graph.DefaultGradientTape
import sk.ainet.lang.graph.DefaultGraphExecutionContext
import sk.ainet.lang.nn.Module
import sk.ainet.lang.nn.dsl.TrainingRunner
import sk.ainet.lang.nn.dsl.sequential
import sk.ainet.lang.nn.dsl.training
import sk.ainet.lang.nn.loss.CategoricalCrossEntropyLoss
import sk.ainet.lang.nn.optim.adam
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.tensor.relu
import sk.ainet.lang.tensor.softmax
import sk.ainet.lang.types.FP32
import java.io.BufferedReader
import java.io.InputStreamReader

const val EPOCHS = 100

fun main() {

    val baseCtx = DirectCpuExecutionContext()

    val (features, labels) = loadKaggleCsv("Iris.csv")
    val split = test_train_split(baseCtx, features, labels, testRatio = 0.2f)
    val numClasses = labels[0].size

    println("X_train shape: ${split.X_train.shape}")
    println("X_test shape:  ${split.X_test.shape}")
    println("Y_train shape: ${split.Y_train.shape}")
    println("Y_test shape:  ${split.Y_test.shape}")

    val trainCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.TRAIN,
        createTapeFactory = { _ -> DefaultGradientTape() }
    )

    val model = sequential<FP32, Float>(trainCtx) {
        input(4)
        dense(8, "hidden") { weights { randn(std = 0.1f) } }
        activation { it.relu() }
        dense(numClasses, "output") { weights { randn(std = 0.1f) } }
        activation { it.softmax() }
    }

    val runner = training<FP32, Float> {
        model { model }
        loss { CategoricalCrossEntropyLoss() }
        optimizer {
            adam().apply {
                model.trainableParameters().forEach { addParameter(it) }
            }
        }
    }

    println("Training...")
    val finalLoss = train(
        baseCtx = baseCtx,
        trainCtx = trainCtx,
        runner = runner,
        X = split.X_train,
        y = split.Y_train,
        epochs = EPOCHS
    )
    println("Final Loss: $finalLoss")

    val evalCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.EVAL
    )

    println("Eval...")
    val accuracy = evaluate(
        baseCtx = baseCtx,
        evalCtx = evalCtx,
        model = model,
        X = split.X_test,
        y = split.Y_test
    )
    println("Test Accuracy: ${accuracy * 100}%")
}

fun train(
    baseCtx: DirectCpuExecutionContext,
    trainCtx: DefaultGraphExecutionContext,
    runner: TrainingRunner<FP32, Float>,
    X: Tensor<FP32, Float>,
    y: Tensor<FP32, Float>,
    epochs: Int = 10
): Float {
    val numSamples = X.shape.dimensions[0]
    val numFeatures = X.shape.dimensions[1]
    val numClasses = y.shape.dimensions[1]

    var lastLoss = 0f
    repeat(epochs) { epoch ->
        var totalLoss = 0f
        for (i in 0 until numSamples) {
            val x = baseCtx.fromFloatArray<FP32, Float>(
                Shape(1, numFeatures), FP32::class,
                FloatArray(numFeatures) { f -> X.data[i, f] }
            )
            val target = trainCtx.fromFloatArray<FP32, Float>(
                Shape(1, numClasses), FP32::class,
                FloatArray(numClasses) { c -> y.data[i, c] }
            )
            val lossTensor = runner.step(trainCtx, x, target)
            totalLoss += lossTensor.data.get()
        }
        lastLoss = totalLoss / numSamples
        if ((epoch + 1) % 10 == 0 || epoch == 0) {
            println("Epoch ${epoch + 1}, Loss: $lastLoss")
        }
    }
    return lastLoss
}

fun evaluate(
    baseCtx: DirectCpuExecutionContext,
    evalCtx: DefaultGraphExecutionContext,
    model: Module<FP32, Float>,
    X: Tensor<FP32, Float>,
    y: Tensor<FP32, Float>
): Float {
    val numSamples = X.shape.dimensions[0]
    val numFeatures = X.shape.dimensions[1]
    val numClasses = y.shape.dimensions[1]

    var correct = 0
    for (i in 0 until numSamples) {
        val sample = baseCtx.fromFloatArray<FP32, Float>(
            Shape(1, numFeatures), FP32::class,
            FloatArray(numFeatures) { f -> X.data[i, f] }
        )
        val output = model.forward(sample, evalCtx)
        val predicted = (0 until numClasses).maxByOrNull { output.data[0, it] } ?: 0
        val actual = (0 until numClasses).maxByOrNull { y.data[i, it] } ?: 0
        if (predicted == actual) correct++
    }
    return correct.toFloat() / numSamples
}

fun loadKaggleCsv(resourcePath: String): Pair<List<FloatArray>, List<FloatArray>> {
    val stream = object {}.javaClass.classLoader.getResourceAsStream(resourcePath)
        ?: error("Resource not found: $resourcePath")

    val lines = BufferedReader(InputStreamReader(stream)).use { reader ->
        reader.readLines()
    }.drop(1).shuffled()

    val featureList = mutableListOf<FloatArray>()
    val labelList = mutableListOf<FloatArray>()

    for (line in lines) {
        if (line.isBlank()) continue
        val attributes = line.split(",")

        featureList.add(
            floatArrayOf(
                attributes[1].toFloat(),
                attributes[2].toFloat(),
                attributes[3].toFloat(),
                attributes[4].toFloat()
            )
        )

        val labelOneHot = when (attributes[5].trim()) {
            "Iris-setosa" -> labelToOneHot(0)
            "Iris-versicolor" -> labelToOneHot(1)
            "Iris-virginica" -> labelToOneHot(2)
            else -> labelToOneHot(0)
        }
        labelList.add(labelOneHot)
    }

    return Pair(featureList, labelList)
}

data class TestTrainSplit(
    val X_train: Tensor<FP32, Float>,
    val X_test: Tensor<FP32, Float>,
    val Y_train: Tensor<FP32, Float>,
    val Y_test: Tensor<FP32, Float>
)

fun test_train_split(
    baseCtx: DirectCpuExecutionContext,
    X: List<FloatArray>,
    y: List<FloatArray>,
    testRatio: Float = 0.1f
): TestTrainSplit {
    require(X.size == y.size) { "X and y must have the same size" }
    require(testRatio in 0.0f..1.0f) { "testRatio must be in betwen 0.0 and 1.0" }

    val testSize = (X.size * testRatio).toInt()
    val trainSize = X.size - testSize

    val X_train = X.take(trainSize)
    val X_test = X.drop(trainSize)
    val y_train = y.take(trainSize)
    val y_test = y.drop(trainSize)

    val numFeatures = X[0].size
    val numClasses = y[0].size

    val X_train_tensor = baseCtx.fromFloatArray<FP32, Float>(
        Shape(trainSize, numFeatures), FP32::class, X_train.flatMap { it.toList() }.toFloatArray()
    )
    val X_test_tensor = baseCtx.fromFloatArray<FP32, Float>(
        Shape(testSize, numFeatures), FP32::class, X_test.flatMap { it.toList() }.toFloatArray()
    )
    val y_train_tensor = baseCtx.fromFloatArray<FP32, Float>(
        Shape(trainSize, numClasses), FP32::class, y_train.flatMap { it.toList() }.toFloatArray()
    )
    val y_test_tensor = baseCtx.fromFloatArray<FP32, Float>(
        Shape(testSize, numClasses), FP32::class, y_test.flatMap { it.toList() }.toFloatArray()
    )

    return TestTrainSplit(X_train_tensor, X_test_tensor, y_train_tensor, y_test_tensor)
}

fun labelToOneHot(classIndex: Int, numClasses: Int = 3): FloatArray {
    val oneHot = FloatArray(numClasses) { 0.0f }
    oneHot[classIndex] = 1.0f
    return oneHot
}
