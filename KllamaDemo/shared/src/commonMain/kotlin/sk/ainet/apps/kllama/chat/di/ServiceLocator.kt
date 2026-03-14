package sk.ainet.apps.kllama.chat.di

import sk.ainet.apps.kllama.chat.data.repository.ModelRepositoryImpl
import sk.ainet.apps.kllama.chat.data.repository.PlatformModelLoader
import sk.ainet.apps.kllama.chat.data.source.ModelDataSource
import sk.ainet.apps.kllama.chat.data.source.ModelMetadataCacheDataSource
import sk.ainet.apps.kllama.chat.data.source.NoOpModelDataSource
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import sk.ainet.apps.kllama.chat.domain.port.ModelRepository
import sk.ainet.apps.kllama.chat.inference.LlamaInferenceEngine
import sk.ainet.apps.kllama.chat.logging.AppLogger

/**
 * Simple service locator for dependency injection.
 * Provides singleton instances of services.
 *
 * Runtime and tokenizer are stored as [Any] because their concrete types
 * (from skainet-kllama / skainet-llm) are only available on JVM.
 */
object ServiceLocator {

    private lateinit var platformLoader: PlatformModelLoader
    private lateinit var modelDataSource: ModelDataSource

    private val metadataCache by lazy { ModelMetadataCacheDataSource() }

    private val _modelRepository: ModelRepository by lazy {
        ModelRepositoryImpl(platformLoader, modelDataSource, metadataCache)
    }

    /** SKaiNET InferenceRuntime<FP32> — stored as Any for commonMain compatibility. */
    private var currentRuntime: Any? = null

    /** SKaiNET Tokenizer (GGUFTokenizer) — stored as Any for commonMain compatibility. */
    private var currentTokenizer: Any? = null

    /**
     * Configure the service locator with platform-specific implementations.
     */
    fun configure(
        loader: PlatformModelLoader,
        dataSource: ModelDataSource = NoOpModelDataSource()
    ) {
        platformLoader = loader
        modelDataSource = dataSource
    }

    /**
     * Get the model repository instance.
     */
    fun getModelRepository(): ModelRepository {
        return _modelRepository
    }

    /**
     * Get the platform loader (for accessing platform-specific features).
     */
    fun getPlatformLoader(): PlatformModelLoader = platformLoader

    /**
     * Set the current InferenceRuntime (called by CommonModelLoader after loading).
     */
    fun setRuntime(runtime: Any?) {
        currentRuntime = runtime
    }

    /**
     * Get the current InferenceRuntime if available.
     */
    fun getRuntime(): Any? = currentRuntime

    /**
     * Set the current Tokenizer (called by CommonModelLoader after loading).
     */
    fun setTokenizer(tokenizer: Any?) {
        currentTokenizer = tokenizer
    }

    /**
     * Get the current Tokenizer if available.
     */
    fun getTokenizer(): Any? = currentTokenizer

    /**
     * Create an inference engine for the given model.
     */
    fun createInferenceEngine(model: LoadedModel?): InferenceEngine {
        return LlamaInferenceEngine(
            model = model,
            runtime = currentRuntime,
            tokenizer = currentTokenizer
        )
    }

    /**
     * Get the inference engine factory.
     */
    fun getInferenceEngineFactory(): (LoadedModel?) -> InferenceEngine {
        return { model -> createInferenceEngine(model) }
    }

    /**
     * Get the AppLogger singleton.
     */
    fun getAppLogger(): AppLogger = AppLogger

    /**
     * Whether the platform supports loading the embedded model from Compose resources.
     * Set to true on JVM Desktop where there is enough memory and inference support.
     * Leave false on Web/WASM (638MB resource would hang the browser).
     */
    var supportsEmbeddedModelLoading: Boolean = false

    /**
     * Check if the service locator has been configured.
     */
    val isInitialized: Boolean
        get() = ::platformLoader.isInitialized

    /**
     * Reset the service locator (for testing).
     */
    fun reset() {
        currentRuntime = null
        currentTokenizer = null
    }
}
