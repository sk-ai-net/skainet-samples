package sk.ainet.apps.kllama.chat.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import sk.ainet.apps.kllama.chat.playground.explainer.ArchitectureDiagram
import sk.ainet.apps.kllama.chat.playground.explainer.AttentionHeatmap
import sk.ainet.apps.kllama.chat.playground.explainer.ResidualBars
import sk.ainet.apps.kllama.chat.playground.explainer.StepSnapshot
import sk.ainet.apps.kllama.chat.playground.explainer.TopKBars

/**
 * The headline tab. Step through Qwen3-0.6B token-by-token and inspect what
 * happens inside: per-layer attention heatmaps, residual activations, and
 * top-K next-token distribution.
 *
 * One-way flow for v1: the runtime's KV-cache state grows as the user
 * advances; there is no in-app "reset" — restart the app to start over.
 */
@Composable
fun ExplainerTab(
    holder: QwenModelHolder,
    state: QwenLoadingState,
) {
    var prompt by remember { mutableStateOf("The quick brown fox") }
    var tokensSoFar by remember { mutableStateOf<IntArray>(intArrayOf()) }
    var promptProcessed by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<StepSnapshot?>(null) }
    var selectedBlock by remember { mutableStateOf(0) }
    var selectedHead by remember { mutableStateOf(0) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Transformer Explainer — what's happening inside Qwen3 as it generates one token at a time.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = prompt,
            onValueChange = {
                if (!promptProcessed) prompt = it
            },
            label = { Text(if (promptProcessed) "Prompt (locked once stepping starts)" else "Prompt") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !promptProcessed && !working,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = state is QwenLoadingState.Ready && !working,
                onClick = {
                    val ready = state as? QwenLoadingState.Ready ?: return@Button
                    working = true
                    scope.launch {
                        try {
                            if (!promptProcessed) {
                                // Initial run: feed all prompt tokens through.
                                val promptTokens = ready.tokenizer.encode(prompt)
                                var snap: StepSnapshot? = null
                                var accum = IntArray(0)
                                for (t in promptTokens) {
                                    snap = holder.stepInstrumented(
                                        tokenId = t,
                                        priorTokens = accum,
                                        temperature = 0f,
                                    )
                                    accum = accum + t
                                }
                                if (snap != null) {
                                    snapshot = snap
                                    tokensSoFar = snap.tokensSoFar
                                    promptProcessed = true
                                }
                            } else {
                                val last = tokensSoFar.last()
                                val snap = holder.stepInstrumented(
                                    tokenId = last,
                                    priorTokens = tokensSoFar,
                                    temperature = 0f,
                                )
                                snapshot = snap
                                tokensSoFar = snap.tokensSoFar
                            }
                        } finally {
                            working = false
                        }
                    }
                },
            ) {
                Text(
                    when {
                        working -> "Running..."
                        !promptProcessed -> "Run prompt"
                        else -> "Step"
                    }
                )
            }

            if (snapshot != null) {
                Text(
                    "Step latency: ${snapshot!!.elapsedMillis} ms",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.wrapContentHeight().padding(top = 12.dp),
                )
            }
        }

        TokenStrip(
            tokensSoFar = tokensSoFar,
            tokenizer = (state as? QwenLoadingState.Ready)?.tokenizer,
        )

        if (state is QwenLoadingState.Ready) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.width(180.dp)) {
                    ArchitectureDiagram(
                        arch = state.architecture,
                        selectedBlock = selectedBlock,
                        onBlockSelected = { selectedBlock = it },
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val attentionForSelected = snapshot?.perLayerAttention?.firstOrNull {
                        it.blockIndex == selectedBlock
                    }
                    AttentionHeatmap(
                        capture = attentionForSelected,
                        selectedHead = selectedHead,
                        onHeadSelected = { selectedHead = it },
                    )
                    val residualForSelected = snapshot?.perLayerResidual?.firstOrNull {
                        it.blockIndex == selectedBlock
                    }
                    ResidualBars(capture = residualForSelected)
                    TopKBars(entries = snapshot?.topK.orEmpty())
                }
            }
        } else {
            Text("Waiting for model to load...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TokenStrip(
    tokensSoFar: IntArray,
    tokenizer: sk.ainet.apps.llm.Tokenizer?,
) {
    if (tokensSoFar.isEmpty() || tokenizer == null) return
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            tokensSoFar.takeLast(40).forEach { id ->
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = displayToken(tokenizer.decode(id)),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}

private fun displayToken(s: String): String = when {
    s.isEmpty() -> "∅"
    s == "\n" -> "\\n"
    s.isBlank() -> "·"
    else -> s
}
