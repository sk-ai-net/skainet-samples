package sk.ainet.samples.kernelrace.worker

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.samples.kernelrace.engine.GenerativeEngine
import sk.ainet.samples.kernelrace.engine.LlmEngine
import sk.ainet.samples.kernelrace.model.ModelData

/**
 * Runs entirely inside a dedicated Web Worker — a genuinely separate JS/Wasm execution context
 * from the main (UI) thread. `generateUntilStop` (SKaiNET's core generation loop, in
 * skainet-transformers-core) is a plain non-suspend function with zero yield points, and
 * kotlinx.coroutines' Dispatchers.Default on wasmJs is not a real thread pool (same single JS
 * thread as Main) — so calling any of this from the main thread blocks page repaint for the
 * full duration of load + every generate() call. Running it here instead means the worker's
 * own thread blocks, not the UI's, and the main thread can repaint between every postMessage.
 *
 * Message protocol (plain strings — see composeApp's main.kt for the main-thread side):
 *   in  "MODEL:<base64 gguf bytes>" -> load the model, reply "STATUS:..." while working, "READY" when done
 *   in  "GENERATE:<prompt>"         -> generate, replying "TOKEN:<piece>" per token, then "DONE:<full text>"
 *   out "ERROR:<message>"           -> either phase failed
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(msg) => self.postMessage(msg)")
external fun postToMain(msg: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(handler) => { self.onmessage = (e) => handler(e.data); }")
external fun installOnMessage(handler: (String) -> Unit)

private val workerScope = CoroutineScope(Job())
private var engine: GenerativeEngine? = null

fun main() {
    installOnMessage { raw -> workerScope.launch { handleMessage(raw) } }
}

@OptIn(ExperimentalEncodingApi::class)
private suspend fun handleMessage(raw: String) {
    try {
        when {
            raw.startsWith("MODEL:") -> {
                postToMain("STATUS:Building runtime…")
                val bytes = Base64.decode(raw.removePrefix("MODEL:"))
                engine = LlmEngine.load(DirectCpuExecutionContext(), ModelData.Bytes(bytes))
                postToMain("READY")
            }
            raw.startsWith("GENERATE:") -> {
                val prompt = raw.removePrefix("GENERATE:")
                val loaded = engine ?: error("worker: GENERATE received before MODEL")
                val text = loaded.generate(prompt) { piece -> postToMain("TOKEN:$piece") }
                postToMain("DONE:$text")
            }
        }
    } catch (t: Throwable) {
        postToMain("ERROR:${t.message ?: t::class.simpleName}")
    }
}
