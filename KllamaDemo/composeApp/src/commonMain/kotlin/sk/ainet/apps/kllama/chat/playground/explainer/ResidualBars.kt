package sk.ainet.apps.kllama.chat.playground.explainer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Bar chart of the residual stream values for the selected block, downsampled
 * to [MAX_RESIDUAL_BARS] buckets. Negative values dip below center, positive
 * rise above.
 */
@Composable
fun ResidualBars(capture: ResidualCapture?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (capture == null) {
            Text(
                "Residual stream for the selected block is not captured yet — run a Step first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Text(
            "Residual · blk.${capture.blockIndex} · dim=${capture.originalDim} (downsampled to ${capture.downsampledValues.size} bars)",
            style = MaterialTheme.typography.labelMedium,
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val values = capture.downsampledValues
            val maxAbs = values.maxOfOrNull { abs(it) } ?: 1f
            val safeMax = if (maxAbs == 0f) 1f else maxAbs
            val barW = size.width / values.size
            val midY = size.height / 2

            // axis
            drawLine(
                color = Color(0xFFB0BEC5),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1f,
            )

            for (i in values.indices) {
                val v = values[i] / safeMax
                val barH = (v.coerceIn(-1f, 1f)) * (size.height / 2)
                val x = i * barW
                val y0 = if (barH >= 0f) midY - barH else midY
                val h = abs(barH)
                drawRect(
                    color = if (barH >= 0f) Color(0xFF1976D2) else Color(0xFFE53935),
                    topLeft = Offset(x + 0.5f, y0),
                    size = Size((barW - 1f).coerceAtLeast(1f), h),
                )
            }
        }
    }
}
