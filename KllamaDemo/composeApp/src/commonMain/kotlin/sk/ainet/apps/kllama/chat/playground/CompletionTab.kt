package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.ainet.apps.llm.generate
import sk.ainet.ui.components.LoadingIndicator

/**
 * Raw completion: prompt → token stream, no chat template. Useful for
 * showing how the base model continues text.
 */
@Composable
fun CompletionTab(state: QwenLoadingState) {
    var prompt by remember { mutableStateOf("Once upon a time in a tiny village,") }
    var output by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(output) { scroll.scrollTo(scroll.maxValue) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt (no template applied)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !generating,
        )

        if (state !is QwenLoadingState.Ready) {
            ModelLoadingPanel(state)
            return@Column
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                enabled = state is QwenLoadingState.Ready && !generating,
                onClick = {
                    val ready = state as? QwenLoadingState.Ready ?: return@Button
                    output = prompt
                    generating = true
                    scope.launch {
                        try {
                            val tokens = ready.tokenizer.encode(prompt)
                            withContext(Dispatchers.Default) {
                                ready.runtime.generate(
                                    prompt = tokens,
                                    steps = 128,
                                    temperature = 0.8f,
                                ) { id -> output += ready.tokenizer.decode(id) }
                            }
                        } finally {
                            generating = false
                        }
                    }
                },
            ) { Text(if (generating) "Generating..." else "Complete") }

            Button(enabled = !generating, onClick = { output = "" }) { Text("Clear") }

            if (generating) {
                LoadingIndicator(size = 24.dp)
            }
        }

        Text(
            text = if (output.isEmpty() && !generating) "(no output yet)" else output,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
