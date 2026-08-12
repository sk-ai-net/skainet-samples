package sk.ainet.samples.kernelrace

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import java.io.File
import java.util.ServiceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import sk.ainet.backend.api.kernel.KernelProvider
import sk.ainet.backend.api.kernel.KernelRegistry
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.exec.kernel.ScalarKernelProvider
import sk.ainet.samples.kernelrace.engine.GenerativeEngine
import sk.ainet.samples.kernelrace.engine.LlmEngine
import sk.ainet.samples.kernelrace.model.AndroidModelProvider
import sk.ainet.samples.kernelrace.platform.logEvent

/** Which kernels the next engine (re)build pins — full-power A/B comparison. */
enum class KernelMode { NEON, SCALAR }

/**
 * Application-scoped model holder: the engine is heavyweight, so it lives for the app lifetime
 * and is dropped under memory pressure.
 *
 * Kernel wiring happens here, once per process, before any matmul:
 * - main process → ServiceLoader discovers the JNI NEON provider from skainet-backend-jni-cpu
 *   (highest priority wins). Done directly because backend-api's KernelServiceLoader is
 *   jvmMain-only — absent from the library's Android variant — while java.util.ServiceLoader
 *   works on ART.
 * - ":scalar" process → [ScalarKernelProvider] is pinned first; every dispatch site only
 *   auto-installs when the registry is EMPTY, so the NEON provider never registers and this
 *   process stays scalar.
 * This is what powers the one-phone split-screen race.
 */
class KernelRaceApp : Application() {

    private val mutex = Mutex()
    private var engine: GenerativeEngine? = null
    private val modelProvider by lazy { AndroidModelProvider(this, hfToken = BuildConfig.HF_TOKEN) }

    val isScalarProcess: Boolean by lazy { currentProcessName().endsWith(":scalar") }

    /** Fullscreen A/B switch: same process, same window, different kernels. */
    var kernelMode: KernelMode = KernelMode.NEON
        private set

    /** Kernel lookups are cached per execution context, so switching modes drops the engine;
     *  the next generate reloads with the new pin. */
    suspend fun setKernelMode(mode: KernelMode) = mutex.withLock {
        if (mode != kernelMode) {
            kernelMode = mode
            engine = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (isScalarProcess) {
            KernelRegistry.register(ScalarKernelProvider)
        } else {
            ServiceLoader.load(KernelProvider::class.java).forEach { KernelRegistry.register(it) }
        }
        logEvent(
            "app_start",
            "process" to if (isScalarProcess) "scalar" else "main",
            "providers" to KernelRegistry.availableNames(),
        )
    }

    suspend fun engine(onProgress: (String) -> Unit = {}): GenerativeEngine = mutex.withLock {
        engine ?: withContext(Dispatchers.IO) {
            val model = modelProvider.resolve(onProgress)
            onProgress("Loading model…")
            val kernels = if (isScalarProcess || kernelMode == KernelMode.SCALAR) "scalar" else "neon"
            logEvent("model_load_start", "kernels" to kernels)
            LlmEngine.load(DirectCpuExecutionContext(), model).also {
                if (isScalarProcess || kernelMode == KernelMode.SCALAR) {
                    // Android's platform ops factory re-registers ServiceLoader providers (JNI
                    // included) on every context creation, which defeats the onCreate pin.
                    // Kernel lookups are lazy — resolved at the FIRST matmul — so re-pin scalar
                    // after the load has constructed the context, before any forward runs.
                    KernelRegistry.clearForTesting()
                    KernelRegistry.register(ScalarKernelProvider)
                }
                // NEON mode needs no action: the load's context creation just re-registered
                // the ServiceLoader providers (JNI at priority 100).
                logEvent("model_load_done", "kernels" to kernels)
            }
        }.also { engine = it }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            engine = null // reloaded lazily on next generate
        }
    }

    private fun currentProcessName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) getProcessName()
        else File("/proc/self/cmdline").readText().substringBefore(0.toChar())
}
