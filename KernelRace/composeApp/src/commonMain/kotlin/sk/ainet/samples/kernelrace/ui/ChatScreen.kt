package sk.ainet.samples.kernelrace.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import sk.ainet.samples.kernelrace.vm.ChatViewModel
import sk.ainet.ui.components.indeterminateOrbitingFadingRingLoader
import kernelrace.composeapp.generated.resources.Res
import kernelrace.composeapp.generated.resources.skainet_logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    skainetVersion: String,
    kernelControls: @Composable (busy: Boolean, viewModel: ChatViewModel, currentPrompt: () -> String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var prompt by remember { mutableStateOf("Explain what a NEON (ARM) instruction is, in two sentences.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.skainet_logo),
                            contentDescription = "SKaiNET logo",
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("SKaiNET", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            skainetVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    state.tokensPerSecond?.let {
                        Text(
                            "${formatFixed1(it)} tok/s",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            kernelControls(state.busy, viewModel) { prompt }

            Text(
                state.status,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
            Spacer(Modifier.height(6.dp))

            val loading = state.busy && state.output.isEmpty()
            if (loading) {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    indeterminateOrbitingFadingRingLoader(size = 120.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = state.output.ifEmpty { "…" },
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Prompt") },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.generate(prompt) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.busy) "Generating…" else "Generate on-device")
            }
        }
    }
}

private fun formatFixed1(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    val whole = rounded.toLong()
    val frac = kotlin.math.round((rounded - whole) * 10).toInt().let { if (it < 0) -it else it }
    return "$whole.$frac"
}
