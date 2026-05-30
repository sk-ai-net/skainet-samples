package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.ainet.ui.components.LoadingIndicator
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Tool-calling demo (experimental). Demonstrates the round-trip:
 *
 *   1. Model receives the user prompt + a tools section listing `get_current_time`.
 *   2. We expect the model to emit a JSON tool call.
 *   3. The host executes the tool locally (Clock.System.now()).
 *   4. The tool result is fed back into a second model turn.
 *   5. The model produces a natural-language answer.
 *
 * Qwen3 doesn't always reliably produce the right JSON without
 * fine-tuning, so this tab also shows the *raw* model output so you
 * can inspect what came back.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun ToolCallTab(state: QwenLoadingState) {
    val userPrompt = "What time is it right now?"
    var modelTurn1 by remember { mutableStateOf("") }
    var toolOutput by remember { mutableStateOf("") }
    var modelTurn2 by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state !is QwenLoadingState.Ready) {
            ModelLoadingPanel(state)
            return@Column
        }
        Text(
            "Experimental — Qwen3-0.6B is small and may not emit clean tool-call JSON without finetuning. This tab is a faithful demo of the round-trip even when the model misbehaves.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("User prompt: \"$userPrompt\"", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Available tool: get_current_time() → ISO timestamp",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
        Button(
            enabled = state is QwenLoadingState.Ready && !running,
            onClick = {
                val ready = state as? QwenLoadingState.Ready ?: return@Button
                modelTurn1 = ""
                toolOutput = ""
                modelTurn2 = ""
                running = true
                scope.launch {
                    try {
                        // Turn 1: ask model to produce a tool call.
                        val system =
                            "You have access to one tool: " +
                            "{\"name\":\"get_current_time\",\"description\":\"Returns the current time as an ISO-8601 timestamp.\",\"parameters\":{}}. " +
                            "When the user asks a question that requires this tool, respond with ONLY a JSON object of the form " +
                            "{\"tool\":\"get_current_time\",\"args\":{}}. Otherwise answer naturally."
                        val turn1Prompt =
                            "<|im_start|>system\n$system<|im_end|>\n" +
                            "<|im_start|>user\n$userPrompt<|im_end|>\n" +
                            "<|im_start|>assistant\n"
                        ready.runtime.generateUntilImEnd(
                            tokenizer = ready.tokenizer,
                            promptTokens = ready.tokenizer.encode(turn1Prompt),
                            maxTokens = 80,
                            temperature = 0.1f,
                            onText = { modelTurn1 += it },
                        )

                        // Execute the tool unconditionally (we know the answer the model wanted).
                        toolOutput = "{\"iso\":\"${Clock.System.now()}\"}"  // wall clock

                        // Turn 2: feed the tool result back, ask for natural-language answer.
                        val turn2Prompt =
                            "<|im_start|>system\nYou are a helpful assistant.<|im_end|>\n" +
                            "<|im_start|>user\n$userPrompt<|im_end|>\n" +
                            "<|im_start|>assistant\nI looked it up via get_current_time which returned $toolOutput. Now I will answer: " +
                            ""
                        ready.runtime.generateUntilImEnd(
                            tokenizer = ready.tokenizer,
                            promptTokens = ready.tokenizer.encode(turn2Prompt),
                            maxTokens = 80,
                            temperature = 0.5f,
                            onText = { modelTurn2 += it },
                        )
                    } finally {
                        running = false
                    }
                }
            },
        ) { Text(if (running) "Running..." else "Run the round-trip") }
            if (running) LoadingIndicator(size = 24.dp)
        }

        Labeled(label = "Model turn 1 (expected: JSON tool call)") {
            Text(
                modelTurn1.ifEmpty { "(not run yet)" },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Labeled(label = "Tool output (host-executed)") {
            Text(
                toolOutput.ifEmpty { "(not run yet)" },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Labeled(label = "Model turn 2 (natural-language answer)") {
            Text(
                modelTurn2.ifEmpty { "(not run yet)" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(8.dp)) { content() }
        }
    }
}
