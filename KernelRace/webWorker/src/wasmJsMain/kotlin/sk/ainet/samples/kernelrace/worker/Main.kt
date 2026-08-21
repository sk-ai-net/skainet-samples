package sk.ainet.samples.kernelrace.worker

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
 * The worker fetches the GGUF itself (see [fetchModelBase64]) instead of receiving it from the
 * main thread — an earlier version had the main thread do `Res.readBytes` + base64-encode +
 * postMessage, which held up to 4 copies of the ~145MB model alive at once across both threads
 * (main-thread bytes, main-thread base64 string, worker's structured-clone copy, worker's decoded
 * bytes) and reliably crashed the renderer around 4GB — a regression the single-threaded
 * synchronous path never had. Fetching independently means only this worker's own memory is ever
 * in play, matching the memory profile already verified safe in the standalone Node harness.
 *
 * Message protocol (plain strings — see composeApp's main.kt for the main-thread side):
 *   in  "LOAD"               -> fetch + load the model, reply "STATUS:..." while working, "READY" when done
 *   in  "GENERATE:<prompt>"  -> generate, replying "TOKEN:<piece>" per token, then "DONE:<full text>"
 *   out "ERROR:<message>"    -> either phase failed
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(msg) => self.postMessage(msg)")
external fun postToMain(msg: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(handler) => { self.onmessage = (e) => handler(e.data); }")
external fun installOnMessage(handler: (String) -> Unit)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(msg) => console.log('[worker] ' + msg)")
external fun workerLog(msg: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(msg) => console.error('[worker] ' + msg)")
external fun workerLogError(msg: String)

// FileReader.readAsDataURL does the bytes->base64 conversion natively (fast, no manual JS byte
// loop) — we only need the payload after the "base64," marker. Matches the base64 round-trip
// already proven memory-safe for this exact model in the standalone Node harness this session.
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    (url, onDone, onError) => {
        fetch(url)
            .then((r) => { if (!r.ok) throw new Error('fetch failed: ' + r.status + ' ' + url); return r.blob(); })
            .then((blob) => new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(String(reader.result).split(',')[1]);
                reader.onerror = () => reject(reader.error);
                reader.readAsDataURL(blob);
            }))
            .then((base64) => onDone(base64))
            .catch((e) => onError(String(e && e.message ? e.message : e)));
    }
    """,
)
external fun fetchAsBase64Async(url: String, onDone: (String) -> Unit, onError: (String) -> Unit)

private suspend fun fetchModelBase64(url: String): String = suspendCancellableCoroutine { cont ->
    fetchAsBase64Async(
        url = url,
        onDone = { cont.resume(it) },
        onError = { cont.resumeWithException(RuntimeException(it)) },
    )
}

// Same static path the main thread's Res.readBytes resolved to (confirmed via the dev server's
// own network log) — served identically whether hit from the main thread or a worker's fetch().
private const val MODEL_URL =
    "/composeResources/kernelrace.composeapp.generated.resources/files/SmolLM2-135M-Instruct-Q8_0.gguf"

private val workerScope = CoroutineScope(Job())
private var engine: GenerativeEngine? = null

fun main() {
    workerLog("worker script started, installing onmessage handler")
    installOnMessage { raw ->
        workerLog("received message: ${raw.take(60)}${if (raw.length > 60) "…(${raw.length} chars)" else ""}")
        workerScope.launch { handleMessage(raw) }
    }
    workerLog("onmessage handler installed, worker ready to receive")
}

@OptIn(ExperimentalEncodingApi::class)
private suspend fun handleMessage(raw: String) {
    try {
        when {
            raw == "LOAD" -> {
                workerLog("LOAD message: fetching model from $MODEL_URL")
                postToMain("STATUS:Downloading model bundle…")
                val base64 = fetchModelBase64(MODEL_URL)
                workerLog("fetched, decoding ${base64.length} base64 chars")
                postToMain("STATUS:Building runtime…")
                val bytes = Base64.decode(base64)
                workerLog("decoded ${bytes.size} bytes, calling LlmEngine.load")
                engine = LlmEngine.load(DirectCpuExecutionContext(), ModelData.Bytes(bytes))
                workerLog("LlmEngine.load returned, posting READY")
                postToMain("READY")
            }
            raw.startsWith("GENERATE:") -> {
                val prompt = raw.removePrefix("GENERATE:")
                workerLog("GENERATE message: prompt=\"$prompt\"")
                val loaded = engine ?: error("worker: GENERATE received before MODEL")
                var tokenCount = 0
                val text = loaded.generate(prompt) { piece ->
                    tokenCount++
                    postToMain("TOKEN:$piece")
                }
                workerLog("generate() returned after $tokenCount tokens, posting DONE")
                postToMain("DONE:$text")
            }
            else -> workerLog("unrecognized message prefix, ignoring: ${raw.take(30)}")
        }
    } catch (t: Throwable) {
        workerLogError("caught exception: ${t::class.simpleName}: ${t.message}")
        postToMain("ERROR:${t.message ?: t::class.simpleName}")
    }
}
