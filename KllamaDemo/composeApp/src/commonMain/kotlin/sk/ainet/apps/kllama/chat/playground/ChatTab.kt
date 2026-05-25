package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.ainet.ui.components.LoadingIndicator

/**
 * Chat tab using Qwen's ChatML template applied inline. Streams tokens as
 * decoded text chunks. Stops on the `<|im_end|>` marker or after maxTokens.
 */
@Composable
fun ChatTab(state: QwenLoadingState) {
    var prompt by remember { mutableStateOf("Hello, can you introduce yourself in one sentence?") }
    var response by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(response) { scroll.scrollTo(scroll.maxValue) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !generating,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                enabled = state is QwenLoadingState.Ready && !generating,
                onClick = {
                    val ready = state as? QwenLoadingState.Ready ?: return@Button
                    response = ""
                    generating = true
                    scope.launch {
                        try {
                            val templated = qwenChatML(systemPrompt = "You are Qwen, a helpful assistant.", user = prompt)
                            val tokens = ready.tokenizer.encode(templated)
                            ready.runtime.generateUntilImEnd(
                                tokenizer = ready.tokenizer,
                                promptTokens = tokens,
                                maxTokens = 256,
                                temperature = 0.7f,
                                onText = { chunk -> response += chunk },
                            )
                        } finally {
                            generating = false
                        }
                    }
                },
            ) { Text(if (generating) "Generating..." else "Send") }

            Button(
                enabled = !generating,
                onClick = { response = "" },
            ) { Text("Clear") }

            if (generating) {
                LoadingIndicator(size = 24.dp)
            }
        }

        when (state) {
            is QwenLoadingState.Ready -> {
                Text(
                    text = if (response.isEmpty() && !generating) "(no response yet)" else response,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is QwenLoadingState.Failed -> Text(
                "Model failed to load: ${state.message}",
                color = MaterialTheme.colorScheme.error,
            )
            else -> ModelLoadingPanel(state)
        }
    }
}

private fun qwenChatML(systemPrompt: String, user: String): String =
    "<|im_start|>system\n$systemPrompt<|im_end|>\n" +
    "<|im_start|>user\n$user<|im_end|>\n" +
    "<|im_start|>assistant\n"
