package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kllamademo.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sk.ainet.ui.components.LoadingIndicator

private const val MODEL_RESOURCE_PATH = "files/qwen3-0.6b-Q4_0.gguf"

/**
 * Top-level Qwen3-0.6B playground host. Owns the singleton [QwenModelHolder]
 * and renders a TabRow with the playground demo modes.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun QwenPlaygroundHost() {
    val holder = remember { QwenModelHolder() }
    val state by holder.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state is QwenLoadingState.Idle) {
            val bytes = withContext(Dispatchers.Default) { Res.readBytes(MODEL_RESOURCE_PATH) }
            holder.load(bytes)
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Visualize", "Tokenizer", "Chat", "Completion", "Translate", "Tool call")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            when (selectedTab) {
                0 -> ExplainerTab(holder = holder, state = state)
                1 -> TokenizerTab(state)
                2 -> ChatTab(state)
                3 -> CompletionTab(state)
                4 -> TranslateTab(state)
                5 -> ToolCallTab(state)
            }
        }

        Footer(state)
    }
}

@Composable
private fun Footer(state: QwenLoadingState) {
    val label = when (state) {
        QwenLoadingState.Idle -> "Idle"
        is QwenLoadingState.Loading -> state.phase
        is QwenLoadingState.Ready -> "Ready — loaded in ${state.loadMillis} ms"
        is QwenLoadingState.Failed -> "Failed at '${state.stage}': ${state.message}"
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state is QwenLoadingState.Loading) {
            LoadingIndicator(size = 36.dp, modifier = Modifier.padding(top = 4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (state is QwenLoadingState.Failed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Model: Qwen3-0.6B (Q3_K_S, ~280 MB) — Apache 2.0 — Alibaba Cloud / Qwen team",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
