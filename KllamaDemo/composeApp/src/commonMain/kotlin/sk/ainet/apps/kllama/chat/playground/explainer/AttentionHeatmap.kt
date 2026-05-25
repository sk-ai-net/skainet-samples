package sk.ainet.apps.kllama.chat.playground.explainer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Heatmap of one attention head's `seqLen × seqLen` post-softmax weights.
 * Cell color goes from dark blue (≈0) to bright yellow (≈max).
 */
@Composable
fun AttentionHeatmap(
    capture: AttentionCapture?,
    selectedHead: Int,
    onHeadSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (capture == null) {
            Text(
                "Attention heatmap is not available for this layer. The SDPA kernel may have fused the softmax — in that case the observer does not see a discrete softmax op.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Text(
            "Attention · blk.${capture.blockIndex} · ${capture.heads} heads · seqLen=${capture.seqLen}",
            style = MaterialTheme.typography.labelMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (h in 0 until capture.heads) {
                SuggestionChip(
                    onClick = { onHeadSelected(h) },
                    label = { Text("h$h") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (h == selectedHead) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }

        Canvas(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            val n = capture.seqLen
            val cell = size.width / n
            val maxV = headMax(capture, selectedHead)

            for (r in 0 until n) {
                for (c in 0 until n) {
                    val v = headValue(capture, selectedHead, r, c)
                    val norm = if (maxV > 0f) v / maxV else 0f
                    drawRect(
                        color = colorFor(norm),
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
        Text(
            "Rows = query position, cols = key position. Brighter = higher attention.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun headValue(c: AttentionCapture, head: Int, r: Int, col: Int): Float {
    val n = c.seqLen
    val perHead = n * n
    val headOffset = head * perHead
    return c.values[headOffset + r * n + col]
}

private fun headMax(c: AttentionCapture, head: Int): Float {
    val n = c.seqLen
    val perHead = n * n
    val headOffset = head * perHead
    var m = 0f
    for (i in headOffset until headOffset + perHead) if (c.values[i] > m) m = c.values[i]
    return m
}

private fun colorFor(norm: Float): Color {
    val v = norm.coerceIn(0f, 1f)
    // viridis-ish: dark blue -> teal -> green -> yellow
    val r = (v * v).coerceIn(0f, 1f)
    val g = v.coerceIn(0f, 1f)
    val b = (1f - v).coerceIn(0f, 1f) * 0.7f + 0.2f
    return Color(red = r, green = g, blue = b, alpha = 1f)
}
