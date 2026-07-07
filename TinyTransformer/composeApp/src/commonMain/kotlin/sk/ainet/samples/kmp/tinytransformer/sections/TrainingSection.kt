package sk.ainet.samples.kmp.tinytransformer.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kmp.tinytransformer.TinyTransformerUiState
import sk.ainet.samples.kmp.tinytransformer.TinyTransformerViewModel
import sk.ainet.samples.kmp.tinytransformer.components.AttentionHeatmap
import sk.ainet.samples.kmp.tinytransformer.components.LabeledSlider
import sk.ainet.samples.kmp.tinytransformer.components.LossCurve
import sk.ainet.samples.kmp.tinytransformer.formatFloat
import sk.ainet.samples.kmp.tinytransformer.i18n.Strings
import kotlin.math.roundToInt

@Composable
fun TrainingSection(
    state: TinyTransformerUiState,
    viewModel: TinyTransformerViewModel,
    strings: Strings,
) {
    SectionCard(title = strings.sectionTraining, badge = strings.sectionTrainingBadge) {
        LabeledSlider(
            label = strings.vocabSizeLabel,
            valueLabel = state.maxVocab.toString(),
            hint = strings.vocabSizeHint,
            value = state.maxVocab.toFloat(),
            valueRange = 5f..80f,
            onValueChange = { viewModel.onMaxVocabChanged(it.roundToInt()) },
            enabled = !state.isTraining,
        )
        LabeledSlider(
            label = strings.contextLengthLabel,
            valueLabel = state.contextLen.toString(),
            hint = strings.contextLengthHint,
            value = state.contextLen.toFloat(),
            valueRange = 3f..16f,
            steps = 12,
            onValueChange = { viewModel.onContextLenChanged(it.roundToInt()) },
            enabled = !state.isTraining,
        )
        LabeledSlider(
            label = strings.epochsLabel,
            valueLabel = state.epochs.toString(),
            hint = strings.epochsHint,
            value = state.epochs.toFloat(),
            valueRange = 50f..500f,
            onValueChange = { viewModel.onEpochsChanged(it.roundToInt()) },
            enabled = !state.isTraining,
        )
        LabeledSlider(
            label = strings.learningRateLabel,
            valueLabel = formatFloat(state.learningRate, 2),
            hint = strings.learningRateHint,
            value = state.learningRate,
            valueRange = 0.01f..1f,
            onValueChange = viewModel::onLearningRateChanged,
            enabled = !state.isTraining,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Button(
                onClick = viewModel::startTraining,
                enabled = state.dataReady && !state.isTraining,
            ) {
                Text(strings.startTraining)
            }
            OutlinedButton(
                onClick = viewModel::stopTraining,
                enabled = state.isTraining,
            ) {
                Text(strings.stopTraining)
            }
            Spacer(Modifier.width(8.dp))
            if (state.epoch > 0) {
                Text(
                    text = strings.epochProgress(state.epoch, state.epochs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(strings.attentionTitle, style = MaterialTheme.typography.titleSmall)
            AttentionHeatmap(state.attention, modifier = Modifier.padding(top = 6.dp))

            Text(
                strings.lossTitle,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            LossCurve(
                lossHistory = state.lossHistory,
                totalEpochs = state.epochs,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}
