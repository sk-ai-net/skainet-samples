package sk.ainet.samples.kernelrace.platform

import android.util.Log
import java.io.File
import sk.ainet.exec.kernel.jni.JniKernelProvider

private const val NUL_CHAR = 0.toChar()

/**
 * NEON tier label from hardware capability (dispatched .so tier), NOT from KernelRegistry —
 * the registry is mutated by mode switches (a scalar-mode run leaves it as scalar until the
 * next load), which would mislabel the NEON chip "SCALAR" after switching back.
 */
actual fun kernelTierLabel(): String =
    if (JniKernelProvider.isAvailable()) "ARM NEON" else "NEON (unavailable)"

actual val supportsKernelRace: Boolean = true

/** ":scalar" vs main process, read once from /proc/self/cmdline — splits log tags so the
 *  one-phone NEON-vs-scalar race can be watched as two separately filterable `adb logcat` streams. */
private val processTag: String by lazy {
    val cmdline = File("/proc/self/cmdline").readText().substringBefore(NUL_CHAR)
    if (cmdline.endsWith(":scalar")) "SCALAR" else "NEON"
}

internal actual fun platformLog(line: String) {
    Log.i("SKAINET_PERF_$processTag", line)
}
