package sk.ainet.apps.kllama.chat.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import sk.ainet.apps.kllama.chat.logging.AppLogger
import androidx.compose.foundation.text.selection.SelectionContainer
import sk.ainet.apps.kllama.chat.logging.LogEntry
import sk.ainet.apps.kllama.chat.logging.LogLevel
import sk.ainet.apps.kllama.chat.domain.model.GenerationState
import sk.ainet.apps.kllama.chat.domain.model.InferenceStatistics
import sk.ainet.apps.kllama.chat.domain.model.ModelLoadingState
import sk.ainet.apps.kllama.chat.ui.BackIcon
import sk.ainet.apps.kllama.chat.ui.ClearIcon
import sk.ainet.apps.kllama.chat.ui.ModelDetailsCard
import sk.ainet.apps.kllama.chat.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(BackIcon, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        SelectionContainer {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = rememberLazyListState()
        ) {
            // Section 1: Model Metadata Card
            item {
                SectionHeader("Model")
                val modelMetadata = (uiState.modelState as? ModelLoadingState.Loaded)?.model?.metadata
                if (modelMetadata != null) {
                    ModelDetailsCard(
                        metadata = modelMetadata,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No model loaded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Section 2: Performance Metrics Card
            item {
                SectionHeader("Performance")
                val loadingTimeMs = (uiState.modelState as? ModelLoadingState.Loaded)?.loadTimeMs ?: 0L
                val statistics = when (val gs = uiState.generationState) {
                    is GenerationState.Generating -> gs.statistics
                    is GenerationState.Complete -> gs.statistics
                    else -> InferenceStatistics()
                }
                PerformanceMetricsCard(
                    loadingTimeMs = loadingTimeMs,
                    prefillTimeMs = statistics.prefillTimeMs,
                    timeToFirstTokenMs = statistics.timeToFirstTokenMs,
                    currentTps = statistics.tokensPerSecond,
                    peakTps = statistics.peakTps,
                    tokensGenerated = statistics.tokensGenerated,
                    totalTimeMs = statistics.totalTimeMs
                )
            }

            // Section 3: Log Entries
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Logs (${logs.size})")
                    TextButton(onClick = { AppLogger.clear() }) {
                        Icon(
                            ClearIcon,
                            contentDescription = "Clear logs",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Clear")
                    }
                }
            }

            if (logs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No log entries yet. Load a model or send a message to see logs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(logs.reversed()) { entry ->
                LogEntryRow(entry)
            }

            // Bottom spacer
            item {
                Box(modifier = Modifier.height(8.dp))
            }
        }
        } // SelectionContainer
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun PerformanceMetricsCard(
    loadingTimeMs: Long,
    prefillTimeMs: Long,
    timeToFirstTokenMs: Long,
    currentTps: Float,
    peakTps: Float,
    tokensGenerated: Int,
    totalTimeMs: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(label = "Load Time", value = formatDuration(loadingTimeMs))
                MetricItem(label = "Prefill", value = formatDuration(prefillTimeMs))
                MetricItem(label = "TTFT", value = formatDuration(timeToFirstTokenMs))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(label = "Current TPS", value = formatTps(currentTps))
                MetricItem(label = "Peak TPS", value = formatTps(peakTps))
                MetricItem(label = "Tokens", value = "$tokensGenerated")
            }

            if (totalTimeMs > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(label = "Total Time", value = formatDuration(totalTimeMs))
                    val avgTps = if (totalTimeMs > 0 && tokensGenerated > 0)
                        tokensGenerated * 1000f / totalTimeMs else 0f
                    MetricItem(label = "Avg TPS", value = formatTps(avgTps))
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.WARN -> Color(0xFFFFA000)
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    var expanded by remember { mutableStateOf(false) }
    val hasDetails = entry.details.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .then(if (hasDetails) Modifier.clickable { expanded = !expanded } else Modifier)
    ) {
        // Color-coded left border
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (expanded && hasDetails) 80.dp else 44.dp)
                .background(levelColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Timestamp
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Tag badge
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = levelColor,
                    modifier = Modifier
                        .background(
                            levelColor.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                // Level
                Text(
                    text = entry.level.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = levelColor
                )
            }

            // Message
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Expandable details
            if (hasDetails) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        entry.details.forEach { (key, value) ->
                            Row {
                                Text(
                                    text = "$key: ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (!expanded) {
                    Text(
                        text = "Tap for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestampMs: Long): String {
    val totalSeconds = timestampMs / 1000
    val hours = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val millis = timestampMs % 1000
    return "${padTwo(hours)}:${padTwo(minutes)}:${padTwo(seconds)}.${padThree(millis)}"
}

private fun padTwo(value: Long): String = if (value < 10) "0$value" else "$value"
private fun padThree(value: Long): String = when {
    value < 10 -> "00$value"
    value < 100 -> "0$value"
    else -> "$value"
}

private fun formatDecimal(value: Double, decimals: Int = 1): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    val intPart = rounded.toLong()
    val fracPart = abs(((rounded - intPart) * factor).toLong())
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}

private fun formatDuration(ms: Long): String {
    if (ms == 0L) return "-"
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

private fun formatTps(tps: Float): String {
    if (tps == 0f) return "-"
    return "${formatDecimal(tps.toDouble(), 2)} tok/s"
}
