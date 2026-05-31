package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import sk.ainet.ui.plot.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SinusVisualization(
    sliderValue: Float,
    actualSinus: Double,
    approximatedSinusMlp: Float,
    approximatedSinusPretrained: Float? = null,
    approximatedSinusTrained: Float? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val pretrainedColor = Color(0xFF673AB7) // Deep Purple for pretrained
    val trainedColor = Color(0xFF4CAF50) // Green for trained

    // Create sine curve data series
    val sineCurvePoints = remember {
        List(101) { i ->
            val normalizedX = i.toFloat() / 100
            val angle = normalizedX * (PI.toFloat() / 2)
            DataPoint(angle, sin(angle.toDouble()).toFloat())
        }
    }
    val sineCurveSeries = DataSeries(
        points = sineCurvePoints,
        color = Color.Gray.copy(alpha = 0.3f),
        strokeWidth = 2f
    )

    val bounds = PlotBounds(0f, PI.toFloat() / 2, 0f, 1f)

    Plot(
        bounds = bounds,
        modifier = modifier.height(220.dp).fillMaxWidth(),
        padding = PlotPadding(left = 40f, right = 10f, top = 10f, bottom = 25f),
        xAxis = AxisConfig(
            tickCount = 3,
            labelFormatter = { x ->
                when {
                    x <= 0f -> "0"
                    x >= PI.toFloat() / 2 - 0.01f -> "π/2"
                    else -> "π/4"
                }
            }
        ),
        yAxis = AxisConfig(
            tickCount = 3,
            labelFormatter = { y ->
                when {
                    y <= 0f -> "0"
                    y >= 1f - 0.01f -> "1.0"
                    else -> "0.5"
                }
            }
        )
    ) {
        // Draw reference sine curve
        drawLineSeries(sineCurveSeries)

        // Calculate positions
        val x = dataToPixelX(sliderValue)
        val actualY = dataToPixelY(actualSinus.toFloat())
        val approximatedYMlp = dataToPixelY(approximatedSinusMlp)

        // Draw vertical dashed line at current x position
        drawScope.drawLine(
            Color.Gray,
            Offset(x, plotArea.top),
            Offset(x, plotArea.bottom),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw error lines (from actual to approximated)
        drawScope.drawLine(
            tertiary.copy(alpha = 0.5f),
            Offset(x, actualY),
            Offset(x, approximatedYMlp),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        // Draw pretrained point if available
        if (approximatedSinusPretrained != null) {
            val approximatedYPretrained = dataToPixelY(approximatedSinusPretrained)
            drawScope.drawLine(
                pretrainedColor.copy(alpha = 0.5f),
                Offset(x, actualY),
                Offset(x, approximatedYPretrained),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            drawScope.drawCircle(pretrainedColor, 6f, Offset(x, approximatedYPretrained))
        }

        // Draw trained point if available
        if (approximatedSinusTrained != null) {
            val approximatedYTrained = dataToPixelY(approximatedSinusTrained)
            drawScope.drawLine(
                trainedColor.copy(alpha = 0.5f),
                Offset(x, actualY),
                Offset(x, approximatedYTrained),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            drawScope.drawCircle(trainedColor, 6f, Offset(x, approximatedYTrained))
        }

        // Draw data points
        drawScope.drawCircle(primary, 6f, Offset(x, actualY))           // Actual sine
        drawScope.drawCircle(tertiary, 6f, Offset(x, approximatedYMlp))  // MLP approximation
    }
}
