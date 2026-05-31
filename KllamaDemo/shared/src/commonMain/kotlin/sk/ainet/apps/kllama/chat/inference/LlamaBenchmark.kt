package sk.ainet.apps.kllama.chat.inference

import sk.ainet.benchmark.BenchmarkRunner
import sk.ainet.benchmark.benchmarkSuite
import sk.ainet.context.DirectCpuExecutionContext

fun createLlamaBenchmarkSuite(runtime: Any) =
    benchmarkSuite("LLaMA Inference") {
        context { DirectCpuExecutionContext() }
        case("Single token forward pass") {
            warmup(3)
            iterations(20)
            setup { platformResetRuntime(runtime) }
            run { platformForwardAndSample(runtime, 1, 0f) }
        }
        case("10-token generation") {
            warmup(2)
            iterations(10)
            setup { platformResetRuntime(runtime) }
            run {
                repeat(10) { platformForwardAndSample(runtime, 1, 0f) }
            }
        }
    }

fun runLlamaBenchmark(runtime: Any) {
    val suite = createLlamaBenchmarkSuite(runtime)
    val results = BenchmarkRunner.runSuite(suite)
    println(results.prettyPrint())
}
