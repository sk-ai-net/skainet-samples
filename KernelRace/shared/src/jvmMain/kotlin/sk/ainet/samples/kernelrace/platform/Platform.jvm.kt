package sk.ainet.samples.kernelrace.platform

import sk.ainet.exec.kernel.PanamaVectorKernelProvider

/**
 * Was hardcoded to "JVM (scalar)" — wrong whenever the Vector API incubator module is loaded
 * (composeApp's :run task always adds --add-modules jdk.incubator.vector, see
 * skainetSimdJvmArgs in composeApp/build.gradle.kts), which is the normal case. Probing
 * PanamaVectorKernelProvider.isAvailable() directly — a static capability check, same
 * pattern as Android's JniKernelProvider.isAvailable() — avoids depending on KernelRegistry
 * being populated yet: ChatViewModel evaluates this at construction time, before
 * DirectCpuExecutionContext.create() has run (which is what triggers registry population via
 * KernelServiceLoader.installAll()), so a registry query here would always see an empty
 * registry and report "scalar" regardless of what's actually about to run.
 */
actual fun kernelTierLabel(): String =
    if (PanamaVectorKernelProvider.isAvailable()) "Panama Vector (SIMD)" else "JVM (scalar)"

actual val supportsKernelRace: Boolean = false

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.DESKTOP

internal actual fun platformLog(line: String) {
    println("[SKAINET_PERF_JVM] $line")
}
