package sk.ainet.demo

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import sk.ainet.backend.api.kernel.KernelRegistry
import sk.ainet.exec.kernel.jni.JniKernelProvider
import sk.ainet.exec.kernel.jni.JniKernels
import sk.ainet.exec.tensor.ops.KernelProfile

data class UiState(
    val status: String = "Model not loaded",
    val kernelTier: String = "",
    val output: String = "",
    val tokensPerSecond: Double? = null,
    val busy: Boolean = false,
    val scalarMode: Boolean = false,
)

/**
 * NEON tier label from hardware capability (dispatched .so tier), NOT from
 * KernelRegistry — the registry is mutated by mode switches (a scalar-mode
 * run leaves it as [scalar] until the next load), which made the NEON chip
 * mislabel itself "SCALAR" after switching back.
 */
private fun neonTierLabel(): String =
    if (JniKernelProvider.isAvailable()) "ARM NEON" else "NEON (unavailable)"

/** Cross-process race trigger: main window broadcasts, scalar window listens. */
const val ACTION_RACE = "sk.ainet.demo.action.RACE"
const val EXTRA_PROMPT = "prompt"

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(
        UiState(kernelTier = if ((app as SkainetDemoApp).isScalarProcess) "SCALAR" else neonTierLabel())
    )
    val state: StateFlow<UiState> = _state

    /**
     * Fullscreen A/B switch for clean screen recordings: the whole phone runs
     * either the NEON or the scalar kernels — no split-screen CPU sharing.
     * Reloads the model on the next generate (kernel lookups are cached per
     * execution context).
     */
    fun setKernelMode(scalar: Boolean) {
        if (_state.value.busy || _state.value.scalarMode == scalar) return
        viewModelScope.launch {
            getApplication<SkainetDemoApp>().setKernelMode(if (scalar) KernelMode.SCALAR else KernelMode.NEON)
            _state.value = _state.value.copy(
                scalarMode = scalar,
                kernelTier = if (scalar) "SCALAR" else neonTierLabel(),
                output = "",
                tokensPerSecond = null,
                status = "Kernel mode: ${if (scalar) "scalar" else "NEON"} — model reloads on next run",
            )
        }
    }

    /** Load the engine (download + weights) without generating — arms a fair race. */
    fun preload() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true)
        viewModelScope.launch {
            try {
                getApplication<SkainetDemoApp>().engine { progress ->
                    _state.value = _state.value.copy(status = progress)
                }
                _state.value = _state.value.copy(busy = false, status = "Model loaded — ready to race")
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, status = "Error: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun generate(prompt: String) {
        if (_state.value.busy || prompt.isBlank()) return
        _state.value = _state.value.copy(busy = true, output = "", tokensPerSecond = null, status = "Loading model…")

        viewModelScope.launch {
            try {
                val engine = getApplication<SkainetDemoApp>().engine { progress ->
                    _state.value = _state.value.copy(status = progress)
                }
                _state.value = _state.value.copy(status = "Generating")
                KernelProfile.reset()

                var tokenCount = 0
                var firstTokenAt = 0L
                withContext(Dispatchers.Default) {
                    engine.generate(prompt) { piece ->
                        val now = SystemClock.elapsedRealtime()
                        if (tokenCount == 0) firstTokenAt = now
                        tokenCount++
                        val elapsedS = (now - firstTokenAt) / 1000.0
                        _state.value = _state.value.copy(
                            output = _state.value.output + piece,
                            // decode tok/s, measured from the first emitted token (excludes prefill)
                            tokensPerSecond = if (elapsedS > 0.5) (tokenCount - 1) / elapsedS else null,
                        )
                    }
                }
                _state.value = _state.value.copy(busy = false, status = "Done — $tokenCount tokens, fully on-device")
                // Hard evidence of which kernels actually ran this generation.
                Log.i("SKAINET_DEMO", "providers=${KernelRegistry.availableNames()}")
                Log.i("SKAINET_DEMO", KernelProfile.report())
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, status = "Error: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }
}
