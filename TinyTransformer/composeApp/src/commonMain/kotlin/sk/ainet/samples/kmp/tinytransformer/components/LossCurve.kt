package sk.ainet.samples.kmp.tinytransformer.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kmp.tinytransformer.formatFloat
import sk.ainet.ui.plot.AxisConfig
import sk.ainet.ui.plot.DataPoint
import sk.ainet.ui.plot.DataSeries
import sk.ainet.ui.plot.LinePlot
import sk.ainet.ui.plot.PlotBounds
import sk.ainet.ui.plot.PlotPadding

/** Cross-entropy loss per epoch, drawn with the skainet-ui plot component. */
@Composable
fun LossCurve(
    lossHistory: List<Float>,
    totalEpochs: Int,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.error

    val points = lossHistory.mapIndexed { index, loss ->
        DataPoint((index + 1).toFloat(), loss)
    }
    val bounds = if (lossHistory.size >= 2) {
        PlotBounds(
            xMin = 1f,
            xMax = totalEpochs.toFloat().coerceAtLeast(2f),
            yMin = lossHistory.min(),
            yMax = lossHistory.max(),
        )
    } else {
        PlotBounds(0f, totalEpochs.toFloat().coerceAtLeast(1f), 0f, 1f)
    }

    LinePlot(
        series = listOf(DataSeries(points, color)),
        modifier = modifier.height(160.dp).fillMaxWidth(),
        bounds = bounds,
        padding = PlotPadding(left = 56f, right = 12f, top = 10f, bottom = 26f),
        xAxis = AxisConfig(tickCount = 4, labelFormatter = { it.toInt().toString() }),
        yAxis = AxisConfig(tickCount = 3, labelFormatter = { formatFloat(it, 2) }),
    )
}
