package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * No-inference tab: types text, shows how Qwen's BPE tokenizer breaks it
 * into token IDs and their decoded text fragments.
 */
@Composable
fun TokenizerTab(state: QwenLoadingState) {
    var input by remember { mutableStateOf("Hello, world!") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Text to tokenize") },
            modifier = Modifier.fillMaxWidth(),
        )

        when (state) {
            QwenLoadingState.Idle, is QwenLoadingState.Loading -> {
                Text("Waiting for tokenizer to load...", style = MaterialTheme.typography.bodyMedium)
            }
            is QwenLoadingState.Failed -> {
                Text("Failed to load tokenizer: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
            is QwenLoadingState.Ready -> {
                val tokens by remember(input, state) {
                    derivedStateOf { state.tokenizer.encode(input).toList() }
                }
                Text(
                    text = "${tokens.size} tokens",
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(tokens) { id ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp,
                        ) {
                            Text(
                                text = "${id.toString().padStart(5)}  ${escape(state.tokenizer.decode(id))}",
                                modifier = Modifier.padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun escape(s: String): String = buildString {
    for (ch in s) {
        when (ch) {
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            ' ' -> append('·')
            else -> append(ch)
        }
    }
}
