package sk.ainet.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.ainet.ui.components.indeterminateOrbitingFadingRingLoader
import sk.ainet.ui.theme.SKaiNETTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SKaiNETTheme { ChatScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var prompt by remember { mutableStateOf("Explain what a NEON (ARM) instruction is, in two sentences.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.skainet_logo),
                            contentDescription = "SKaiNET logo",
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("SKaiNET", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            BuildConfig.SKAINET_VERSION,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Bottom).padding(bottom = 4.dp),
                        )
                    }
                },
                actions = {
                    state.tokensPerSecond?.let {
                        Text(
                            "%.1f tok/s".format(it),
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
            val context0 = androidx.compose.ui.platform.LocalContext.current
            val inScalarProcess = context0.applicationContext
                .let { it as? SkainetDemoApp }?.isScalarProcess == true
            if (inScalarProcess) {
                AssistChip(onClick = {}, label = { Text(state.kernelTier) })
            } else {
                // Fullscreen A/B switch: whole phone on one kernel path —
                // record one run per mode and compare.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !state.scalarMode,
                        onClick = { viewModel.setKernelMode(false) },
                        enabled = !state.busy,
                        label = { Text(if (state.scalarMode) "NEON" else state.kernelTier) },
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = state.scalarMode,
                        onClick = { viewModel.setKernelMode(true) },
                        enabled = !state.busy,
                        label = { Text("SCALAR") },
                    )
                }
            }
            // Status on its own line — with large font scales it doesn't fit
            // next to the chips and used to blow up the row height.
            Text(
                state.status,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
            Spacer(Modifier.height(6.dp))

            val loading = state.busy && state.output.isEmpty()
            if (loading) {
                // SKaiNET fading-ring loader while the model downloads/loads
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

            val context = androidx.compose.ui.platform.LocalContext.current
            val isMainProcess = context.applicationContext
                .let { it as? SkainetDemoApp }?.isScalarProcess == false
            if (isMainProcess) {
                Spacer(Modifier.height(4.dp))
                // Saveable: entering split-screen recreates the Activity, and plain
                // remember{} would silently disarm the race button.
                var raceArmed by rememberSaveable { mutableStateOf(false) }
                // Tap 1: open the scalar twin adjacent and preload BOTH engines.
                // Tap 2: broadcast the prompt so both windows start in the same instant.
                TextButton(
                    onClick = {
                        if (!raceArmed) {
                            context.startActivity(
                                Intent(context, ScalarActivity::class.java).addFlags(
                                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            )
                            viewModel.preload()
                            raceArmed = true
                        } else {
                            context.sendBroadcast(
                                Intent(ACTION_RACE)
                                    .setPackage(context.packageName)
                                    .putExtra(EXTRA_PROMPT, prompt)
                            )
                            viewModel.generate(prompt)
                        }
                    },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (raceArmed) "Start race 🏁" else "Race against scalar (split screen)")
                }
            }
        }
    }
}
