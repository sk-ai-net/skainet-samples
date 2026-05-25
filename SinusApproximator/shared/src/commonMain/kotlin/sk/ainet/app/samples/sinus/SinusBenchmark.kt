package sk.ainet.app.samples.sinus

import sk.ainet.benchmark.BenchmarkRunner
import sk.ainet.benchmark.benchmarkSuite
import sk.ainet.context.DirectCpuExecutionContext

fun createSinusBenchmarkSuite(mlpCalculator: MLPSinusCalculator) =
    benchmarkSuite("Sinus Approximation") {
        context { DirectCpuExecutionContext() }
        case("MLP inference") {
            warmup(10)
            iterations(100)
            run {
                mlpCalculator.calculate(1.0f)
            }
        }
    }

fun runSinusBenchmark(mlpCalculator: MLPSinusCalculator) {
    val suite = createSinusBenchmarkSuite(mlpCalculator)
    val results = BenchmarkRunner.runSuite(suite)
    println(results.prettyPrint())
}
