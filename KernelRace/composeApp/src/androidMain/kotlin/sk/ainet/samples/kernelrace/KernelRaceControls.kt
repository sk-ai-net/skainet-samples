package sk.ainet.samples.kernelrace

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.ainet.samples.kernelrace.platform.kernelTierLabel
import sk.ainet.samples.kernelrace.vm.ChatViewModel

const val ACTION_RACE = "sk.ainet.samples.kernelrace.action.RACE"
const val EXTRA_PROMPT = "prompt"

/**
 * The Android-only race UI, injected into the platform-neutral [sk.ainet.samples.kernelrace.ui.ChatScreen]
 * via its `kernelControls` slot. In the `:scalar` process this collapses to a read-only chip
 * (that window is a race participant, not a driver); the main process gets the NEON/SCALAR
 * switch plus the split-screen race button.
 */
@Composable
fun KernelRaceControls(busy: Boolean, viewModel: ChatViewModel, currentPrompt: () -> String) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as KernelRaceApp }

    if (app.isScalarProcess) {
        AssistChip(onClick = {}, label = { Text("SCALAR") })
        return
    }

    val scope = rememberCoroutineScope()
    var scalarMode by rememberSaveable { mutableStateOf(app.kernelMode == KernelMode.SCALAR) }

    // Fullscreen A/B switch: whole phone on one kernel path — record one run per mode and compare.
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = !scalarMode,
            onClick = {
                scope.launch {
                    app.setKernelMode(KernelMode.NEON)
                    scalarMode = false
                    viewModel.onKernelModeChanged(kernelTierLabel(), scalarMode = false)
                }
            },
            enabled = !busy,
            label = { Text(if (scalarMode) "NEON" else kernelTierLabel()) },
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = scalarMode,
            onClick = {
                scope.launch {
                    app.setKernelMode(KernelMode.SCALAR)
                    scalarMode = true
                    viewModel.onKernelModeChanged("SCALAR", scalarMode = true)
                }
            },
            enabled = !busy,
            label = { Text("SCALAR") },
        )
    }

    Spacer(Modifier.height(4.dp))

    // Saveable: entering split-screen recreates the Activity, and plain remember{} would
    // silently disarm the race button.
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
                        .putExtra(EXTRA_PROMPT, currentPrompt())
                )
                viewModel.generate(currentPrompt())
            }
        },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (raceArmed) "Start race 🏁" else "Race against scalar (split screen)")
    }
}
