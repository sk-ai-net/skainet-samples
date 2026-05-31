package sk.ainet.apps.kllama.chat.ui

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.apps.kllama.chat.domain.model.InferenceStatistics
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata

/**
 * Panel showing real-time inference statistics.
 */
@Composable
fun StatisticsPanel(
    statistics: InferenceStatistics,
    modelMetadata: ModelMetadata?,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Model info
            modelMetadata?.let { metadata ->
                Text(
                    text = metadata.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(label = "Size", value = metadata.formattedSize)
                    StatItem(label = "Params", value = metadata.formattedParamCount)
                    StatItem(label = "Quant", value = metadata.quantization.name)
                }
            }

            // Generation statistics
            AnimatedVisibility(
                visible = isGenerating || statistics.tokensGenerated > 0,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isGenerating) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Tokens",
                            value = statistics.tokensGenerated.toString()
                        )
                        StatItem(
                            label = "Speed",
                            value = statistics.formattedTps,
                            highlight = isGenerating
                        )
                        StatItem(
                            label = "Time",
                            value = formatDuration(statistics.totalTimeMs)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single statistic item.
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = if (highlight) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Formats a decimal number with the specified number of decimal places.
 * Multiplatform-compatible replacement for String.format("%.Xf", value).
 */
private fun formatDecimal(value: Double, decimals: Int = 1): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    val intPart = rounded.toLong()
    val fracPart = abs(((rounded - intPart) * factor).toLong())
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}

/**
 * Format milliseconds as human-readable duration.
 */
private fun formatDuration(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${formatDecimal(ms / 1000.0)}s"
        else -> {
            val minutes = ms / 60000
            val seconds = (ms % 60000) / 1000
            "${minutes}m ${seconds}s"
        }
    }
}
