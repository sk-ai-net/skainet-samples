package sk.ainet.apps.kllama.chat.ui

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel

/**
 * Card displaying a discovered GGUF model file with optional metadata details.
 */
@Composable
fun DiscoveredModelCard(
    model: DiscoveredModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(model.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )

                // Fade in metadata details when they become available
                AnimatedVisibility(
                    visible = model.metadata != null,
                    enter = fadeIn()
                ) {
                    model.metadata?.let { meta ->
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetadataBadge(text = meta.quantization.name)
                            MetadataBadge(text = "${meta.numLayers}L")
                            MetadataBadge(text = meta.formattedParamCount)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "${formatDecimal(bytes / 1_000_000_000.0)} GB"
        bytes >= 1_000_000 -> "${formatDecimal(bytes / 1_000_000.0)} MB"
        bytes >= 1_000 -> "${formatDecimal(bytes / 1_000.0)} KB"
        else -> "$bytes B"
    }
}

private fun formatDecimal(value: Double, decimals: Int = 2): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    val intPart = rounded.toLong()
    val fracPart = abs(((rounded - intPart) * factor).toLong())
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}
