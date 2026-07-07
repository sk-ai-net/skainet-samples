package sk.ainet.samples.kmp.tinytransformer.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kmp.tinytransformer.TinyTransformerUiState
import sk.ainet.samples.kmp.tinytransformer.formatFloat
import sk.ainet.samples.kmp.tinytransformer.i18n.Strings

@Composable
fun EmbeddingsSection(
    state: TinyTransformerUiState,
    strings: Strings,
) {
    SectionCard(title = strings.sectionEmbeddings, badge = strings.sectionEmbeddingsBadge) {
        Text(strings.embeddingsTableTitle, style = MaterialTheme.typography.titleSmall)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 6.dp),
        ) {
            Row {
                Text(
                    strings.tokenHeader,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.25f),
                )
                Text(
                    strings.vectorHeader,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.75f),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            for ((token, vector) in state.embeddings) {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        token,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.25f),
                    )
                    Text(
                        vector.joinToString(", ", "[", ", …]") { formatFloat(it, 3) },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.75f),
                    )
                }
            }
            if (state.embeddings.isEmpty()) {
                Text(
                    "–",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
