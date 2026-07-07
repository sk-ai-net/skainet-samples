package sk.ainet.samples.kmp.tinytransformer

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** Cross-platform fixed-decimals formatting ("%.3f" is JVM-only). */
fun formatFloat(value: Float, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val scaled = (value * factor).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val magnitude = abs(scaled)
    val integer = magnitude / factor.toLong()
    val fraction = (magnitude % factor.toLong()).toString().padStart(decimals, '0')
    return "$sign$integer.$fraction"
}
