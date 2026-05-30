package sk.ainet.apps.kllama.chat.playground.explainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Top-K candidate next tokens as horizontal probability bars.
 */
@Composable
fun TopKBars(entries: List<TopKEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Top-${entries.size} next-token candidates",
            style = MaterialTheme.typography.labelMedium,
        )
        if (entries.isEmpty()) {
            Text("(no candidates — run a Step first)", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        val maxProb = entries.first().probability.coerceAtLeast(1e-6f)
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = escapeForDisplay(entry.text).padEnd(14).take(14),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(130.dp),
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .height(18.dp)
                        .fillMaxWidth(entry.probability / maxProb),
                ) {}
                Text(
                    text = " ${(entry.probability * 100f).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

private fun escapeForDisplay(s: String): String = buildString {
    for (ch in s) {
        when (ch) {
            '\n' -> append("\\n")
            '\t' -> append("\\t")
            ' ' -> append('·')
            else -> append(ch)
        }
    }
}
