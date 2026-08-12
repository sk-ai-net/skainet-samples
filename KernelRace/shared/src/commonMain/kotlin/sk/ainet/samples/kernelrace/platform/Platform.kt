package sk.ainet.samples.kernelrace.platform

import kotlin.time.TimeSource

/** Human-readable label for the kernel path this platform/process is running — shown in the UI. */
expect fun kernelTierLabel(): String

/** Only Android can pin a JNI NEON provider vs a scalar one and race them side by side. */
expect val supportsKernelRace: Boolean

/** Where the platform actually writes a log line — `adb logcat` on Android, stdout elsewhere. */
internal expect fun platformLog(line: String)

private val processStart = TimeSource.Monotonic.markNow()

/**
 * One-line structured perf events, greppable with `adb logcat -s SKAINET_PERF_NEON:I` on
 * Android (see the platform actual for the process-tagged variant), or read straight off
 * stdout on desktop/wasm.
 */
fun logEvent(name: String, vararg details: Pair<String, Any?>) {
    val line = buildString {
        append("event=").append(name)
        append(" | +").append(processStart.elapsedNow().inWholeMilliseconds).append("ms")
        for ((key, value) in details) {
            if (value != null) append(" | ").append(key).append('=').append(value)
        }
    }
    platformLog(line)
}
