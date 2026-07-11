package sk.ainet.samples.vec2text.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.ainet.lang.types.FP32
import kotlin.math.abs
import kotlin.math.min

private sealed interface Load {
    data object Loading : Load
    data class Missing(val dir: String, val files: List<String>) : Load
    data class Ready(val engine: Vec2TextEngine) : Load
    data class Failed(val message: String) : Load
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Embedding Inversion — vec2text on SKaiNET") {
        MaterialTheme { App() }
    }
}

@Composable
private fun App() {
    var load by remember { mutableStateOf<Load>(Load.Loading) }
    LaunchedEffect(Unit) {
        val dir = Vec2TextEngine.modelsDir()
        val missing = Vec2TextEngine.missing(dir)
        load = if (missing.isNotEmpty()) {
            Load.Missing(dir.absolutePath, missing)
        } else try {
            Load.Ready(withContext(Dispatchers.Default) { Vec2TextEngine.load(dir) })
        } catch (e: Throwable) {
            Load.Failed(e.message ?: e.toString())
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Embedding Inversion", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Decode a sentence embedding back into text (vec2text on SKaiNET). Embeddings are not anonymous.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        when (val l = load) {
            is Load.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(20.dp)); Spacer(Modifier.height(8.dp))
                Text("  Loading models (~1.1 GB)…")
            }
            is Load.Missing -> Text(
                "Models not found in ${l.dir}\nMissing: ${l.files.joinToString()}\n" +
                    "Put the converted weights there or set VEC2TEXT_MODELS_DIR (see README).",
                color = MaterialTheme.colorScheme.error,
            )
            is Load.Failed -> Text("Failed to load models: ${l.message}", color = MaterialTheme.colorScheme.error)
            is Load.Ready -> Tabs(l.engine)
        }
    }
}

@Composable
private fun Tabs(engine: Vec2TextEngine) {
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Round trip", "Vector arithmetic")
    TabRow(selectedTabIndex = tab) {
        titles.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
    }
    Spacer(Modifier.height(16.dp))
    when (tab) {
        0 -> RoundTripTab(engine)
        else -> VectorArithmeticTab(engine)
    }
}

@Composable
private fun RoundTripTab(engine: Vec2TextEngine) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("jack morris is a phd student at cornell tech in new york city") }
    var steps by remember { mutableStateOf(5f) }
    var running by remember { mutableStateOf(false) }
    var vector by remember { mutableStateOf<FloatArray?>(null) }
    val trace = remember { mutableStateListOf<Step>() }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Text to embed & invert") })
        StepsSlider(steps) { steps = it }
        RunButton("Embed → invert", running) {
            running = true; trace.clear(); vector = null
            scope.launch {
                withContext(Dispatchers.Default) {
                    val target = engine.embed(text)
                    vector = engine.toFloats(target)
                    engine.invert(target, steps.toInt()) { trace.add(it) }
                }
                running = false
            }
        }
        vector?.let { EmbeddingStrip(it) }
        Reconstruction(original = text, trace = trace)
    }
}

@Composable
private fun VectorArithmeticTab(engine: Vec2TextEngine) {
    val scope = rememberCoroutineScope()
    var a by remember { mutableStateOf("the weather in paris is cold and rainy") }
    var b by remember { mutableStateOf("the food in tokyo is fresh and delicious") }
    var alpha by remember { mutableStateOf(0.5f) }
    var running by remember { mutableStateOf(false) }
    var vector by remember { mutableStateOf<FloatArray?>(null) }
    val trace = remember { mutableStateListOf<Step>() }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(a, { a = it }, Modifier.fillMaxWidth(), label = { Text("Sentence A") })
        OutlinedTextField(b, { b = it }, Modifier.fillMaxWidth(), label = { Text("Sentence B") })
        Text("Interpolation  A ${"%.2f".format(1 - alpha)}  ↔  B ${"%.2f".format(alpha)}")
        Slider(alpha, { alpha = it }, valueRange = 0f..1f)
        RunButton("Invert interpolated embedding", running) {
            running = true; trace.clear(); vector = null
            scope.launch {
                withContext(Dispatchers.Default) {
                    val target = engine.interpolate(engine.embed(a), engine.embed(b), alpha)
                    vector = engine.toFloats(target)
                    engine.invert(target, 5) { trace.add(it) }
                }
                running = false
            }
        }
        Text(
            "Inverting a blend of two sentence embeddings — text you never wrote, decoded from a vector.",
            style = MaterialTheme.typography.bodySmall,
        )
        vector?.let { EmbeddingStrip(it) }
        Reconstruction(original = "(interpolated embedding)", trace = trace)
    }
}

@Composable
private fun StepsSlider(steps: Float, onChange: (Float) -> Unit) {
    Text("Correction steps: ${steps.toInt()}")
    Slider(steps, onChange, valueRange = 0f..20f, steps = 19)
}

@Composable
private fun RunButton(label: String, running: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onClick, enabled = !running) { Text(label) }
        if (running) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(Modifier.height(18.dp).padding(start = 12.dp))
            Text("  working… (CPU decode is slow)")
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** A horizontal strip visualizing the 768-d embedding: one column per (downsampled) dim, red<0<blue. */
@Composable
private fun EmbeddingStrip(v: FloatArray) {
    Spacer(Modifier.height(12.dp))
    Text("embedding (${v.size}-d)", style = MaterialTheme.typography.labelMedium)
    val maxAbs = (v.maxOfOrNull { abs(it) } ?: 1f).coerceAtLeast(1e-6f)
    Canvas(Modifier.fillMaxWidth().height(48.dp)) {
        val cols = min(size.width.toInt(), v.size)
        val cw = size.width / cols
        for (i in 0 until cols) {
            val value = v[i * v.size / cols] / maxAbs
            val c = if (value >= 0) Color(0f, 0.35f, 1f, value.coerceIn(0f, 1f))
            else Color(1f, 0.2f, 0.2f, (-value).coerceIn(0f, 1f))
            drawRect(c, topLeft = Offset(i * cw, 0f), size = androidx.compose.ui.geometry.Size(cw + 1, size.height))
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Reconstruction(original: String, trace: List<Step>) {
    if (trace.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Text("original:      $original", style = MaterialTheme.typography.bodyMedium)
    val best = trace.maxByOrNull { it.cosine }
    if (best != null) {
        Text("reconstructed: ${best.text}", style = MaterialTheme.typography.titleMedium)
        Text("best cosine:   ${"%.4f".format(best.cosine)}")
    }
    Spacer(Modifier.height(8.dp))
    Text("steps", style = MaterialTheme.typography.labelMedium)
    Column {
        for (s in trace) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${s.step}", Modifier.height(20.dp))
                CosineBar(s.cosine)
                Text("  ${"%.3f".format(s.cosine)}  ${s.text}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CosineBar(cosine: Float) {
    Canvas(Modifier.height(12.dp).fillMaxWidth(0.18f).padding(horizontal = 8.dp)) {
        drawRect(Color(0.85f, 0.85f, 0.85f), size = size)
        drawRect(
            Color(0.2f, 0.6f, 0.3f),
            size = androidx.compose.ui.geometry.Size(size.width * cosine.coerceIn(0f, 1f), size.height),
        )
    }
}
