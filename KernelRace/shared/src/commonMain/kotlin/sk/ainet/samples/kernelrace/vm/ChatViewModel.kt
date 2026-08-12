package sk.ainet.samples.kernelrace.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.ainet.samples.kernelrace.domain.TokenStats
import sk.ainet.samples.kernelrace.domain.UiState
import sk.ainet.samples.kernelrace.engine.GenerativeEngine
import sk.ainet.samples.kernelrace.platform.kernelTierLabel

/**
 * Drives model loading + generation. Holds no Android/JVM/wasm-specific state: how the model
 * bytes are resolved (Android assets/download, desktop cache, bundled wasm bytes) and how the
 * execution context + kernel pinning are set up is entirely the caller's job, injected as a
 * suspend lambda — which is what makes this class unit-testable with a fake on every target.
 *
 * [generationDispatcher] defaults to [Dispatchers.Default] (real CPU-bound work off the UI
 * thread) but is overridable so tests can run generation on the same virtual-time scheduler
 * as [androidx.lifecycle.viewModelScope]'s Main dispatcher.
 */
class ChatViewModel(
    private val loadModel: suspend (onProgress: (String) -> Unit) -> GenerativeEngine,
    initialKernelTier: String = kernelTierLabel(),
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState(kernelTier = initialKernelTier))
    val state: StateFlow<UiState> = _state

    private var engine: GenerativeEngine? = null

    fun generate(prompt: String) {
        if (_state.value.busy || prompt.isBlank()) return
        _state.value = _state.value.copy(busy = true, output = "", tokensPerSecond = null, status = "Loading model…")

        viewModelScope.launch {
            try {
                val loaded = engine ?: loadModel { progress ->
                    _state.value = _state.value.copy(status = progress)
                }.also { engine = it }
                _state.value = _state.value.copy(status = "Generating")

                val stats = TokenStats()
                val clock = TimeSource.Monotonic.markNow()
                withContext(generationDispatcher) {
                    loaded.generate(prompt) { piece ->
                        val nowMs = clock.elapsedNow().inWholeMilliseconds
                        stats.onToken(nowMs)
                        _state.value = _state.value.copy(
                            output = _state.value.output + piece,
                            tokensPerSecond = stats.tokensPerSecond(nowMs),
                        )
                    }
                }
                _state.value = _state.value.copy(
                    busy = false,
                    status = "Done — ${stats.tokenCount} tokens, fully on-device",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, status = "Error: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /** Loads the engine without generating — arms a fair race (Android split-screen only). */
    fun preload() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            try {
                engine = loadModel { progress -> _state.value = _state.value.copy(status = progress) }
                _state.value = _state.value.copy(busy = false, status = "Model loaded — ready to race")
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, status = "Error: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /** Drops the cached engine so the next generate reloads under a different kernel pin
     *  (Android's fullscreen NEON/SCALAR switch calls this after re-pinning the registry). */
    fun onKernelModeChanged(newKernelTier: String, scalarMode: Boolean) {
        if (_state.value.busy) return
        engine = null
        _state.value = _state.value.copy(
            scalarMode = scalarMode,
            kernelTier = newKernelTier,
            output = "",
            tokensPerSecond = null,
            status = "Kernel mode changed — model reloads on next run",
        )
    }
}
