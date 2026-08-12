package sk.ainet.samples.kernelrace.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStatsTest {

    @Test
    fun noRateBeforeAnyToken() {
        val stats = TokenStats()

        assertNull(stats.tokensPerSecond(nowMs = 0))
        assertEquals(0, stats.tokenCount)
    }

    @Test
    fun noRateWithinFirstHalfSecond() {
        val stats = TokenStats()

        stats.onToken(nowMs = 0)
        stats.onToken(nowMs = 200)
        stats.onToken(nowMs = 400)

        // elapsed since the first token is only 400ms — below the 0.5s noise guard
        assertNull(stats.tokensPerSecond(nowMs = 400))
    }

    @Test
    fun excludesPrefillAndFirstTokenFromTheRate() {
        val stats = TokenStats()

        // "prefill" — arrives long before the first decoded token — must not skew the clock
        stats.onToken(nowMs = 5_000)
        // 4 more tokens over exactly 1 second after the first
        stats.onToken(nowMs = 5_250)
        stats.onToken(nowMs = 5_500)
        stats.onToken(nowMs = 5_750)
        stats.onToken(nowMs = 6_000)

        // 5 tokens total, but the rate is measured over 4 decode intervals in 1s => 4 tok/s
        assertEquals(5, stats.tokenCount)
        assertEquals(4.0, stats.tokensPerSecond(nowMs = 6_000))
    }
}
