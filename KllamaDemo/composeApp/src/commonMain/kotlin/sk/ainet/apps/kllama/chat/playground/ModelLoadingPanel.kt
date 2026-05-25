package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.ui.components.LoadingIndicator

/**
 * In-tab placeholder shown while the model is being loaded. Reused by
 * every inference tab so the model-load progress is visible no matter
 * which tab the user lands on first.
 */
@Composable
fun ModelLoadingPanel(state: QwenLoadingState, modifier: Modifier = Modifier) {
    val phase = when (state) {
        QwenLoadingState.Idle -> "Waiting to start..."
        is QwenLoadingState.Loading -> state.phase
        is QwenLoadingState.Failed -> "Failed: ${state.message}"
        is QwenLoadingState.Ready -> "Ready."
    }
    val isError = state is QwenLoadingState.Failed
    val isBusy = state is QwenLoadingState.Idle || state is QwenLoadingState.Loading

    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isBusy) {
            LoadingIndicator(size = 56.dp)
        }
        Text(
            text = phase,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isBusy) {
            Text(
                text = "Loading once on app launch — every tab shares the same model.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
