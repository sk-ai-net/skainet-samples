package sk.ainet.samples.glove.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sk.ainet.ui.components.LoadingIndicator
import sk.ainet.ui.theme.SKaiNETTheme
import sk.ainet.ui.theme.ThemeController

/**
 * Offline embeddings explorer: type a word to see its nearest neighbours, or run vector
 * algebra such as `king - man + woman ≈ queen`. The vectors are bundled in the app, so
 * everything works with no network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun App() {
    // A ThemeController lets the user flip dark/light on every platform (the SKaiNET
    // look defaults to its signature dark, red-accented theme).
    val themeController = remember { ThemeController() }

    SKaiNETTheme(themeController = themeController) {
        val vm = remember { EmbeddingsViewModel() }
        LaunchedEffect(Unit) { vm.loadEmbeddings() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("GloVe Embeddings Explorer") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    actions = {
                        IconButton(onClick = { themeController.toggleTheme() }) {
                            Text(if (themeController.isDarkTheme) "☀️" else "🌙")
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                when (val s = vm.load) {
                    is LoadState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Loading bundled vectors…")
                    }
                    is LoadState.Failed -> Text(
                        "Failed to load embeddings: ${s.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    is LoadState.Ready -> ReadyContent(vm, s)
                }
            }
        }
    }
}

@Composable
private fun ReadyContent(vm: EmbeddingsViewModel, ready: LoadState.Ready) {
    var tab by remember { mutableStateOf(0) }

    Text(
        "${ready.vocabSize} words · ${ready.dim}-dim vectors (bundled, offline)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    TabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Neighbours") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Analogy") })
    }
    Spacer(Modifier.height(16.dp))

    when (tab) {
        0 -> NeighboursTab(vm)
        1 -> AnalogyTab(vm)
    }

    Spacer(Modifier.height(16.dp))
    Text("Available words:", style = MaterialTheme.typography.labelMedium)
    Text(
        vm.words.joinToString(" "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NeighboursTab(vm: EmbeddingsViewModel) {
    var query by remember { mutableStateOf("king") }
    val results = remember(query) { vm.neighbors(query) }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Word") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    ResultList(results, emptyHint = "Type a known word to see its nearest neighbours.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalogyTab(vm: EmbeddingsViewModel) {
    var a by remember { mutableStateOf("king") }
    var b by remember { mutableStateOf("man") }
    var c by remember { mutableStateOf("woman") }
    val results = remember(a, b, c) { vm.analogy(a, b, c) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WordField(a, { a = it }, "A", Modifier.weight(1f))
        WordField(b, { b = it }, "− B", Modifier.weight(1f))
        WordField(c, { c = it }, "+ C", Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "${a.trim()} − ${b.trim()} + ${c.trim()}  ≈  ?",
        style = MaterialTheme.typography.titleMedium,
        fontFamily = FontFamily.Monospace,
    )
    Spacer(Modifier.height(12.dp))
    ResultList(results, emptyHint = "Enter three known words (e.g. king − man + woman).")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun ResultList(results: List<Pair<String, Float>>, emptyHint: String) {
    if (results.isEmpty()) {
        Text(emptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        results.forEach { (word, sim) -> SimilarityRow(word, sim) }
    }
}

@Composable
private fun SimilarityRow(word: String, similarity: Float) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(word, modifier = Modifier.width(110.dp), fontWeight = FontWeight.Medium)
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(similarity.coerceIn(0f, 1f))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(formatSim(similarity), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Trim to 3 decimals without relying on platform String.format (unavailable on wasm). */
private fun formatSim(value: Float): String {
    val scaled = (value * 1000).toInt()
    val whole = scaled / 1000
    val frac = (if (scaled < 0) -scaled else scaled) % 1000
    return "$whole.${frac.toString().padStart(3, '0')}"
}
