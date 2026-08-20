package sk.ainet.samples.kernelrace.platform

import platform.Foundation.NSLog

/** No hardware-capability probe here (unlike Android's JniKernelProvider.isAvailable()) —
 *  the native-cinterop provider is a single archive with runtime FEAT_DotProd dispatch
 *  baked in, so there's no separate tier to report. */
actual fun kernelTierLabel(): String = "Apple NEON (cinterop)"

/** The two-process split-screen race is an Android-only mechanism (a second `:scalar`
 *  process re-pinning the kernel registry) — no equivalent process-spawn API on iOS. */
actual val supportsKernelRace: Boolean = false

actual val currentSamplePlatform: SamplePlatform = SamplePlatform.IOS

internal actual fun platformLog(line: String) {
    NSLog("SKAINET_PERF_IOS: %s", line)
}
