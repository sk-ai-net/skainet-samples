package sk.ainet.samples.kernelrace.platform

actual fun kernelTierLabel(): String = "JVM (scalar)"

actual val supportsKernelRace: Boolean = false

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.DESKTOP

internal actual fun platformLog(line: String) {
    println("[SKAINET_PERF_JVM] $line")
}
