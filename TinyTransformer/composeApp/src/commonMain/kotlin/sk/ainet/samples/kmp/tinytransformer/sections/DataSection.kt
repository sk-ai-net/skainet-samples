package sk.ainet.samples.kmp.tinytransformer.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kmp.tinytransformer.TinyTransformerUiState
import sk.ainet.samples.kmp.tinytransformer.TinyTransformerViewModel
import sk.ainet.samples.kmp.tinytransformer.components.KpiTile
import sk.ainet.samples.kmp.tinytransformer.i18n.Strings

@Composable
fun DataSection(
    state: TinyTransformerUiState,
    viewModel: TinyTransformerViewModel,
    strings: Strings,
) {
    SectionCard(title = strings.sectionData, badge = strings.sectionDataBadge) {
        OutlinedTextField(
            value = state.corpusText,
            onValueChange = viewModel::onCorpusChanged,
            label = { Text(strings.corpusLabel) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Button(onClick = viewModel::tokenize, enabled = !state.isTraining) {
                Text(strings.tokenizeButton)
            }
            OutlinedButton(onClick = viewModel::onResetCorpus, enabled = !state.isTraining) {
                Text(strings.exampleDataButton)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
        ) {
            KpiTile(strings.kpiVocabulary, state.vocabSize?.toString() ?: "–", Modifier.weight(1f))
            KpiTile(strings.kpiWindows, state.windowCount?.toString() ?: "–", Modifier.weight(1f))
            KpiTile(strings.kpiContextLength, if (state.dataReady) state.contextLen.toString() else "–", Modifier.weight(1f))
        }
    }
}

/** Shared card chrome for the four numbered sections. */
@Composable
fun SectionCard(
    title: String,
    badge: String,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                )
                androidx.compose.material3.SuggestionChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(badge) },
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        }
    }
}
