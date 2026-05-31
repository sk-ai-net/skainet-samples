package sk.ainet.samples.glove

import kotlin.math.abs

/**
 * Step 2 of the GloVe pipeline: count how often words appear near each other.
 *
 * See `glove.md` §2. Nearby words get stronger weight (1 / distance), which is the
 * first core idea of GloVe: meaning comes from context.
 */
public data class PairKey(val center: Int, val context: Int)

/**
 * Build weighted co-occurrence counts over [tokenIds] using a symmetric window of
 * [windowSize] tokens on each side.
 */
public fun buildCooccurrences(
    tokenIds: List<Int>,
    windowSize: Int,
): Map<PairKey, Float> {
    val counts = mutableMapOf<PairKey, Float>()

    for (i in tokenIds.indices) {
        val center = tokenIds[i]

        val start = maxOf(0, i - windowSize)
        val end = minOf(tokenIds.lastIndex, i + windowSize)

        for (j in start..end) {
            if (i == j) continue

            val context = tokenIds[j]
            val distance = abs(i - j)

            // Nearby words get stronger weight.
            val weight = 1.0f / distance.toFloat()

            val key = PairKey(center, context)
            // getOrDefault is a JVM-only Map method; (counts[key] ?: 0f) is the
            // portable common-code form that also compiles for JS/wasm/native.
            counts[key] = (counts[key] ?: 0f) + weight
        }
    }

    return counts
}

/**
 * Build co-occurrences over multiple independent [sentences], summing the counts
 * from each. Windows never cross a sentence boundary, so a word at the end of one
 * line does not co-occur with the start of the next (which would otherwise produce
 * spurious pairs, including a word co-occurring with itself across the boundary).
 */
public fun buildCooccurrencesOverSentences(
    sentences: List<List<Int>>,
    windowSize: Int,
): Map<PairKey, Float> {
    val counts = mutableMapOf<PairKey, Float>()
    for (sentence in sentences) {
        for ((key, weight) in buildCooccurrences(sentence, windowSize)) {
            counts[key] = (counts[key] ?: 0f) + weight
        }
    }
    return counts
}
