package sk.ainet.samples.kernelrace

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kernelrace.composeapp.generated.resources.Res
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import sk.ainet.samples.kernelrace.engine.GenerativeEngine
import sk.ainet.samples.kernelrace.vm.ChatViewModel

// The webWorker module's raw (unwebpacked) compileSync output — every file it needs
// (.wasm, import-object.mjs, js-builtins.mjs, custom-formatters.js) sits alongside it, and the
// repo root is already served statically by composeApp's own dev-server webpack config (one of
// its three static dirs resolves to the project root), so this path needs no extra build wiring.
// `{type: "module"}` is required: the emitted .mjs uses top-level `import`.
private const val WORKER_SCRIPT_URL =
    "/webWorker/build/compileSync/wasmJs/main/developmentExecutable/kotlin/KernelRace-webWorker.mjs"

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(url) => new Worker(url, { type: 'module' })")
external fun createModuleWorker(url: String): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(worker, msg) => worker.postMessage(msg)")
external fun workerPostMessage(worker: JsAny, msg: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(worker, handler) => { worker.onmessage = (e) => handler(e.data); }")
external fun installWorkerOnMessage(worker: JsAny, handler: (String) -> Unit)

/**
 * Bridges the model-loading/generation Worker (see webWorker/.../Main.kt for the protocol and
 * why this exists — Dispatchers.Default isn't real threading on wasmJs, so this is the only way
 * to keep the page responsive during load/generation) to the [GenerativeEngine] contract
 * [sk.ainet.samples.kernelrace.vm.ChatViewModel] expects. Only one call is ever in flight at a
 * time (ChatViewModel guards on `busy`), so routing every incoming message to whichever call is
 * "current" is safe — no per-call message tagging needed.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private class WorkerGenerativeEngine(private val worker: JsAny) : GenerativeEngine {
    var currentOnToken: ((String) -> Unit)? = null
    var currentCompletion: CompletableDeferred<String>? = null

    override suspend fun generate(prompt: String, maxTokens: Int, onToken: (String) -> Unit): String {
        val completion = CompletableDeferred<String>()
        currentOnToken = onToken
        currentCompletion = completion
        workerPostMessage(worker, "GENERATE:$prompt")
        try {
            return completion.await()
        } finally {
            currentOnToken = null
            currentCompletion = null
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalEncodingApi::class, ExperimentalWasmJsInterop::class)
fun main() {
    ComposeViewport(document.body!!) {
        val viewModel = remember {
            val worker = createModuleWorker(WORKER_SCRIPT_URL)
            val engine = WorkerGenerativeEngine(worker)
            var loadCompletion: CompletableDeferred<Unit>? = null
            var onProgress: ((String) -> Unit)? = null

            installWorkerOnMessage(worker) { raw ->
                when {
                    raw.startsWith("STATUS:") -> onProgress?.invoke(raw.removePrefix("STATUS:"))
                    raw == "READY" -> loadCompletion?.complete(Unit)
                    raw.startsWith("TOKEN:") -> engine.currentOnToken?.invoke(raw.removePrefix("TOKEN:"))
                    raw.startsWith("DONE:") -> engine.currentCompletion?.complete(raw.removePrefix("DONE:"))
                    raw.startsWith("ERROR:") -> {
                        val message = raw.removePrefix("ERROR:")
                        loadCompletion?.completeExceptionally(RuntimeException(message))
                        engine.currentCompletion?.completeExceptionally(RuntimeException(message))
                    }
                }
            }

            ChatViewModel(loadModel = { progress ->
                onProgress = progress
                progress("Downloading model bundle…")
                val bytes = Res.readBytes("files/SmolLM2-135M-Instruct-Q8_0.gguf")
                progress("Sending model to background worker…")
                val ready = CompletableDeferred<Unit>()
                loadCompletion = ready
                workerPostMessage(worker, "MODEL:${Base64.encode(bytes)}")
                ready.await()
                engine
            })
        }
        App(viewModel = viewModel, skainetVersion = SKAINET_VERSION)
    }
}
