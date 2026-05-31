package sk.ainet.apps.kllama.chat.screens

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import sk.ainet.ui.components.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.ainet.apps.kllama.chat.data.file.FilePicker
import sk.ainet.apps.kllama.chat.data.file.FilePickerResult
import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel
import sk.ainet.apps.kllama.chat.domain.model.ModelDiscoveryState
import sk.ainet.apps.kllama.chat.domain.model.ModelLoadingState
import sk.ainet.apps.kllama.chat.ui.BackIcon
import sk.ainet.apps.kllama.chat.ui.CloseIcon
import sk.ainet.apps.kllama.chat.ui.DiscoveredModelCard
import sk.ainet.apps.kllama.chat.ui.FolderIcon
import sk.ainet.apps.kllama.chat.ui.ModelIcon

/**
 * Screen for selecting and loading a model.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    onModelSelected: (FilePickerResult) -> Unit,
    onDiscoveredModelSelected: (DiscoveredModel) -> Unit,
    onNavigateBack: () -> Unit,
    modelState: ModelLoadingState,
    discoveryState: ModelDiscoveryState,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedFile by remember { mutableStateOf<FilePickerResult?>(null) }
    val filePicker = remember { FilePicker() }

    val isLoading = modelState is ModelLoadingState.ParsingMetadata
            || modelState is ModelLoadingState.LoadingWeights
            || modelState is ModelLoadingState.InitializingRuntime

    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = { Text("Load Model") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = BackIcon,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Phased loading progress
            when (modelState) {
                is ModelLoadingState.ParsingMetadata -> {
                    LoadingProgressCard("Parsing metadata: ${modelState.fileName}")
                }
                is ModelLoadingState.LoadingWeights -> {
                    LoadingProgressCard("${modelState.phase}: ${modelState.fileName}")
                }
                is ModelLoadingState.InitializingRuntime -> {
                    LoadingProgressCard("Initializing runtime: ${modelState.fileName}")
                }
                else -> {}
            }

            // Discovered models section
            when (discoveryState) {
                is ModelDiscoveryState.Scanning -> {
                    LoadingIndicator(size = 32.dp)
                    Text(
                        text = "Scanning for GGUF models...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ModelDiscoveryState.Found -> {
                    if (discoveryState.models.isNotEmpty()) {
                        Text(
                            text = "Discovered Models",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discoveryState.models, key = { it.path }) { model ->
                                DiscoveredModelCard(
                                    model = model,
                                    onClick = { onDiscoveredModelSelected(model) }
                                )
                            }
                        }
                    } else {
                        // No models found — show instructions
                        Text(
                            text = "No GGUF models found in the working directory.\nSelect a model file manually or place .gguf files in the app directory.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                is ModelDiscoveryState.Error -> {
                    Text(
                        text = "Discovery error: ${discoveryState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                is ModelDiscoveryState.Idle -> {
                    Text(
                        text = "Select a GGUF model file to load for local inference.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // File picker button (always available as fallback)
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val result = filePicker.pickFile()
                        selectedFile = result
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = FolderIcon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Browse Files")
            }

            // Selected file display
            selectedFile?.let { file ->
                SelectedFileCard(
                    file = file,
                    onClear = { selectedFile = null }
                )
            }

            // Load button for manually selected file
            if (selectedFile != null) {
                Button(
                    onClick = {
                        selectedFile?.let { file ->
                            onModelSelected(file)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        LoadingIndicator(size = 24.dp)
                        Text(
                            text = "Loading...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else {
                        Icon(
                            imageVector = ModelIcon,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Load Model")
                    }
                }
            }

            // Error display
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Supported formats info
            SupportedFormatsInfo()
        }
    }
}

/**
 * Card showing phased loading progress.
 */
@Composable
private fun LoadingProgressCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LoadingIndicator(size = 24.dp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Card showing selected file information.
 */
@Composable
private fun SelectedFileCard(
    file: FilePickerResult,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = formatFileSize(file.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = CloseIcon,
                    contentDescription = "Clear selection",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Info card about supported model formats.
 */
@Composable
private fun SupportedFormatsInfo(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Supported Formats",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GGUF (.gguf) - Quantized models (Q4_K, Q5_K, Q8_0, etc.)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = "Recommended: TinyLlama, Phi-2, or other small models for testing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatDecimal(value: Double, decimals: Int = 2): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    val intPart = rounded.toLong()
    val fracPart = abs(((rounded - intPart) * factor).toLong())
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "${formatDecimal(bytes / 1_000_000_000.0)} GB"
        bytes >= 1_000_000 -> "${formatDecimal(bytes / 1_000_000.0)} MB"
        bytes >= 1_000 -> "${formatDecimal(bytes / 1_000.0)} KB"
        else -> "$bytes B"
    }
}
