package sk.ainet.demo

import android.os.Debug
import android.os.SystemClock
import android.util.Log

/**
 * One-line structured perf events for the demo, greppable with:
 *
 *   adb logcat -s SKAINET_PERF_NEON:I SKAINET_PERF_SCALAR:I
 *
 * The tag carries the process suffix (NEON vs SCALAR — see [ProcessTag]), so
 * the one-phone race can be watched as two side-by-side terminal windows:
 *
 *   adb logcat -s SKAINET_PERF_NEON:I     # window 1
 *   adb logcat -s SKAINET_PERF_SCALAR:I   # window 2
 *
 * Every line carries the offset since process start plus a memory snapshot
 * (ART heap used/max, native heap, total PSS), e.g.:
 *
 *   event=model_load_done | +8123ms | heap=41/256MB | native=163MB | pss=310MB | durMs=6210 | kernels=neon
 *
 * Logcat itself prefixes the wall-clock timestamp.
 */
object PerfLog {

    val TAG = "SKAINET_PERF_${ProcessTag.suffix}"

    private const val MB = 1024 * 1024L

    private val processStart = SystemClock.elapsedRealtime()

    fun event(name: String, vararg details: Pair<String, Any?>) {
        val line = buildString {
            append("event=").append(name)
            append(" | +").append(SystemClock.elapsedRealtime() - processStart).append("ms")
            append(" | ").append(memorySnapshot())
            for ((key, value) in details) {
                if (value != null) append(" | ").append(key).append('=').append(value)
            }
        }
        Log.i(TAG, line)
    }

    /** Reading PSS costs a few ms — fine per event, never call per token. */
    private fun memorySnapshot(): String {
        val rt = Runtime.getRuntime()
        val heapUsed = (rt.totalMemory() - rt.freeMemory()) / MB
        val heapMax = rt.maxMemory() / MB
        val native = Debug.getNativeHeapAllocatedSize() / MB
        val pssMb = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }.totalPss / 1024
        return "heap=$heapUsed/${heapMax}MB | native=${native}MB | pss=${pssMb}MB"
    }
}
