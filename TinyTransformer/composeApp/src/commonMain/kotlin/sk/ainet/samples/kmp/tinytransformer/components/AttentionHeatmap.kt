package sk.ainet.samples.kmp.tinytransformer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import sk.ainet.app.samples.tinytransformer.AttentionSnapshot

/**
 * The T×T causal self-attention heatmap with token labels on both axes —
 * the visual centerpiece of the demo, mirroring the original page's canvas.
 */
@Composable
fun AttentionHeatmap(
    snapshot: AttentionSnapshot?,
    modifier: Modifier = Modifier,
) {
    val cellColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Canvas(modifier = modifier.widthIn(max = 460.dp).fillMaxWidth().aspectRatio(1.15f)) {
        if (snapshot == null || snapshot.size == 0) return@Canvas
        val t = snapshot.size

        val labelSpaceLeft = size.width * 0.18f
        val labelSpaceBottom = size.height * 0.16f
        val plotWidth = size.width - labelSpaceLeft
        val plotHeight = size.height - labelSpaceBottom
        val cellW = plotWidth / t
        val cellH = plotHeight / t

        var maxValue = 1e-9f
        for (v in snapshot.matrix) if (v > maxValue) maxValue = v

        for (i in 0 until t) {
            for (j in 0 until t) {
                val v = snapshot.matrix[i * t + j] / maxValue
                drawRect(
                    color = lerp(emptyColor, cellColor, v.coerceIn(0f, 1f)),
                    topLeft = Offset(labelSpaceLeft + j * cellW, i * cellH),
                    size = Size(cellW, cellH),
                )
            }
        }
        // grid lines
        for (k in 0..t) {
            drawLine(
                color = gridColor,
                start = Offset(labelSpaceLeft, k * cellH),
                end = Offset(labelSpaceLeft + plotWidth, k * cellH),
                strokeWidth = Stroke.HairlineWidth,
            )
            drawLine(
                color = gridColor,
                start = Offset(labelSpaceLeft + k * cellW, 0f),
                end = Offset(labelSpaceLeft + k * cellW, plotHeight),
                strokeWidth = Stroke.HairlineWidth,
            )
        }
        // row labels (left)
        for (i in 0 until t) {
            val token = snapshot.tokens.getOrElse(i) { "" }
            val layout = textMeasurer.measure(token, labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    labelSpaceLeft - layout.size.width - 4f,
                    i * cellH + (cellH - layout.size.height) / 2f,
                ),
            )
        }
        // column labels (bottom, rotated)
        for (j in 0 until t) {
            val token = snapshot.tokens.getOrElse(j) { "" }
            val layout = textMeasurer.measure(token, labelStyle)
            val pivot = Offset(labelSpaceLeft + j * cellW + cellW / 2f, plotHeight + 6f)
            rotate(degrees = -35f, pivot = pivot) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(pivot.x - layout.size.width, pivot.y),
                )
            }
        }
    }
}
