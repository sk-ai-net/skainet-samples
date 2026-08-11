package sk.ainet.demo

import java.io.File

/**
 * ":scalar" vs main process, read once from /proc/self/cmdline (works pre-
 * and post-API 28, no Context needed — usable from plain objects like
 * [PerfLog]). Splits log tags so the one-phone NEON-vs-scalar race can be
 * watched as two separately filterable `adb logcat` streams.
 */
internal object ProcessTag {
    private val NUL_BYTE = 0.toChar()

    val isScalar: Boolean by lazy {
        File("/proc/self/cmdline").readText().substringBefore(NUL_BYTE).endsWith(":scalar")
    }
    val suffix: String by lazy { if (isScalar) "SCALAR" else "NEON" }
}
