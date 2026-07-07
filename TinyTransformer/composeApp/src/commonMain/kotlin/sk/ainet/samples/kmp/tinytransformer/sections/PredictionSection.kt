package sk.ainet.samples.kmp.tinytransformer.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kmp.tinytransformer.TinyTransformerUiState
import sk.ainet.samples.kmp.tinytransformer.TinyTransformerViewModel
import sk.ainet.samples.kmp.tinytransformer.formatFloat
import sk.ainet.samples.kmp.tinytransformer.i18n.Strings

@Composable
fun PredictionSection(
    state: TinyTransformerUiState,
    viewModel: TinyTransformerViewModel,
    strings: Strings,
) {
    SectionCard(title = strings.sectionPrediction, badge = strings.sectionPredictionBadge) {
        OutlinedTextField(
            value = state.prompt,
            onValueChange = viewModel::onPromptChanged,
            label = { Text(strings.promptLabel) },
            placeholder = { Text(strings.promptPlaceholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = viewModel::predict,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Text(strings.predictButton)
        }

        val error = state.predictionError
        if (error != null) {
            Text(
                text = when (error) {
                    "tokenize" -> strings.pleaseTokenizeFirst
                    "train" -> strings.pleaseTrainFirst
                    else -> strings.couldNotTokenize
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        val prediction = state.prediction
        if (prediction != null) {
            Text(
                strings.resultLabel,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                strings.predictions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            for ((token, probability) in prediction.topK) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Text(
                        token,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(110.dp),
                    )
                    LinearProgressIndicator(
                        progress = { probability },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "p=${formatFloat(probability, 3)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            prediction.topK.firstOrNull()?.let { (best, _) ->
                Text(
                    strings.promptPlusBestWord,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    "${state.prompt.trim()} $best",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
