package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class Direction(val label: String, val systemPrompt: String) {
    EN_TO_ZH(
        "English → Chinese",
        "You are a professional translator. Translate the following English text to Chinese (Simplified). Output only the translation, no commentary.",
    ),
    ZH_TO_EN(
        "Chinese → English",
        "You are a professional translator. Translate the following Chinese text to English. Output only the translation, no commentary.",
    ),
}

/**
 * Translation tab: showcases Qwen3's bilingual English ↔ Chinese strength.
 * Uses the ChatML template internally; the system prompt instructs the model
 * to emit only the translation.
 */
@Composable
fun TranslateTab(state: QwenLoadingState) {
    var direction by remember { mutableStateOf(Direction.EN_TO_ZH) }
    var input by remember { mutableStateOf("Hello, how are you today?") }
    var output by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Direction.entries.forEach { d ->
                Button(
                    enabled = !generating && d != direction,
                    onClick = { direction = d },
                ) { Text(d.label) }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Text to translate") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !generating,
        )

        Button(
            enabled = state is QwenLoadingState.Ready && !generating,
            onClick = {
                val ready = state as? QwenLoadingState.Ready ?: return@Button
                output = ""
                generating = true
                scope.launch {
                    try {
                        val templated =
                            "<|im_start|>system\n${direction.systemPrompt}<|im_end|>\n" +
                            "<|im_start|>user\n$input<|im_end|>\n" +
                            "<|im_start|>assistant\n"
                        val tokens = ready.tokenizer.encode(templated)
                        ready.runtime.generateUntilImEnd(
                            tokenizer = ready.tokenizer,
                            promptTokens = tokens,
                            maxTokens = 256,
                            temperature = 0.3f,
                            onText = { chunk -> output += chunk },
                        )
                    } finally {
                        generating = false
                    }
                }
            },
        ) { Text(if (generating) "Translating..." else "Translate") }

        Text(
            text = if (output.isEmpty() && !generating) "(translation will appear here)" else output,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
