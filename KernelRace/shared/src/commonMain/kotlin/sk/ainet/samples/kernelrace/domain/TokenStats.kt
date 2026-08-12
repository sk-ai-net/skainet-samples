package sk.ainet.samples.kernelrace.domain

/**
 * Running decode-speed estimate from streamed token arrival times.
 *
 * Excludes prefill: the clock starts at the *first* emitted token, not at
 * generation start, so prompt-processing time never pollutes the tok/s
 * number. Also excludes the first token itself from the count (there is no
 * decode interval before it), and requires at least half a second of signal
 * before reporting a rate — otherwise a couple of fast tokens produce a
 * wildly noisy estimate.
 */
class TokenStats {
    private var firstTokenAtMs: Long = -1
    var tokenCount: Int = 0
        private set

    /** Call once per emitted token with the current elapsed time (any monotonic clock, ms). */
    fun onToken(nowMs: Long) {
        if (tokenCount == 0) firstTokenAtMs = nowMs
        tokenCount++
    }

    /** Decode tokens/sec so far, or null if there isn't enough signal yet. */
    fun tokensPerSecond(nowMs: Long): Double? {
        if (tokenCount == 0 || firstTokenAtMs < 0) return null
        val elapsedS = (nowMs - firstTokenAtMs) / 1000.0
        if (elapsedS <= 0.5) return null
        return (tokenCount - 1) / elapsedS
    }
}
