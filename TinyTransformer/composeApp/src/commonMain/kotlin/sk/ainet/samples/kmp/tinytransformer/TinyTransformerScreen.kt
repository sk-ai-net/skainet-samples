package sk.ainet.samples.kmp.tinytransformer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kmp.tinytransformer.i18n.Strings
import sk.ainet.samples.kmp.tinytransformer.sections.DataSection
import sk.ainet.samples.kmp.tinytransformer.sections.EmbeddingsSection
import sk.ainet.samples.kmp.tinytransformer.sections.PredictionSection
import sk.ainet.samples.kmp.tinytransformer.sections.TrainingSection

@Composable
fun TinyTransformerScreen(
    viewModel: TinyTransformerViewModel,
    state: TinyTransformerUiState,
    strings: Strings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 760.dp),
        ) {
            DataSection(state, viewModel, strings)
            TrainingSection(state, viewModel, strings)
            EmbeddingsSection(state, strings)
            PredictionSection(state, viewModel, strings)
        }
    }
}
