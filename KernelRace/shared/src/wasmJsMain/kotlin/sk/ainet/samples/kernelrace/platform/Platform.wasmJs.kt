package sk.ainet.samples.kernelrace.platform

actual fun kernelTierLabel(): String = "Wasm (scalar)"

actual val supportsKernelRace: Boolean = false

internal actual fun platformLog(line: String) {
    println("[SKAINET_PERF_WASM] $line")
}
