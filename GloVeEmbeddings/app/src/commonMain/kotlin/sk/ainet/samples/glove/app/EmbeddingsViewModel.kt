package sk.ainet.samples.glove.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sk.ainet.samples.glove.Embeddings
import sk.ainet.samples.glove.GloVeTextReader
import sk.ainet.samples.glove.app.resources.Res

/** Status of the one-time, offline load of the bundled vectors. */
public sealed interface LoadState {
    public data object Loading : LoadState
    public data class Ready(val vocabSize: Int, val dim: Int) : LoadState
    public data class Failed(val message: String) : LoadState
}

/**
 * Loads the embeddings bundled as a Compose resource (fully offline) and exposes
 * nearest-neighbour and analogy lookups for the UI. All maths lives in the reusable
 * [Embeddings] type from the `:glove` library.
 */
public class EmbeddingsViewModel : ViewModel() {
    public var load: LoadState by mutableStateOf(LoadState.Loading)
        private set

    private var embeddings: Embeddings? = null

    /** Sorted vocabulary, shown to the user so they pick words that exist. */
    public val words: List<String>
        get() = embeddings?.vocab?.wordToId?.keys?.sorted() ?: emptyList()

    @OptIn(ExperimentalResourceApi::class)
    public fun loadEmbeddings() {
        if (embeddings != null) return
        viewModelScope.launch {
            load = try {
                val bytes = Res.readBytes("files/mini-glove.50d.txt")
                val parsed = GloVeTextReader.parse(bytes.decodeToString())
                embeddings = parsed
                LoadState.Ready(parsed.vocab.size, parsed.dim)
            } catch (t: Throwable) {
                LoadState.Failed(t.message ?: t.toString())
            }
        }
    }

    public fun isKnown(word: String): Boolean =
        embeddings?.vocab?.wordToId?.containsKey(word.normalizeWord()) == true

    public fun neighbors(query: String, topK: Int = 8): List<Pair<String, Float>> {
        val e = embeddings ?: return emptyList()
        val w = query.normalizeWord()
        if (w.isEmpty() || w !in e.vocab.wordToId) return emptyList()
        return e.nearestWords(w, topK)
    }

    public fun analogy(a: String, b: String, c: String, topK: Int = 8): List<Pair<String, Float>> {
        val e = embeddings ?: return emptyList()
        val aa = a.normalizeWord(); val bb = b.normalizeWord(); val cc = c.normalizeWord()
        if (listOf(aa, bb, cc).any { it.isEmpty() || it !in e.vocab.wordToId }) return emptyList()
        return e.analogy(aa, bb, cc, topK)
    }

    private fun String.normalizeWord(): String = trim().lowercase()
}
