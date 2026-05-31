package sk.ainet.apps.kllama.chat.domain.port

import kotlinx.io.Source
import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata

/**
 * Result of a model load operation.
 */
sealed class ModelLoadResult {
    data class Success(val model: LoadedModel) : ModelLoadResult()
    data class Error(val message: String, val cause: Throwable? = null) : ModelLoadResult()
}

/**
 * Port interface for model repository operations.
 * Handles loading, unloading, and metadata extraction for models.
 */
interface ModelRepository {
    /**
     * Load a model from the given source.
     *
     * @param path Path to the model file
     * @param format Expected model format (auto-detected if UNKNOWN)
     * @return Result containing the loaded model or error information
     */
    suspend fun loadModel(
        path: String,
        format: ModelFormat = ModelFormat.UNKNOWN
    ): ModelLoadResult

    /**
     * Load a model from a Source (for in-memory or stream loading).
     *
     * @param source Source to read the model from
     * @param name Name to use for the model
     * @param sizeBytes Size of the model in bytes
     * @param format Expected model format
     * @return Result containing the loaded model or error information
     */
    suspend fun loadModel(
        source: Source,
        name: String,
        sizeBytes: Long,
        format: ModelFormat
    ): ModelLoadResult

    /**
     * Get the currently loaded model, if any.
     */
    fun getLoadedModel(): LoadedModel?

    /**
     * Unload the current model, freeing resources.
     */
    fun unloadModel()

    /**
     * Extract metadata from a model file without fully loading it.
     *
     * @param path Path to the model file
     * @return Model metadata or null if extraction fails
     */
    suspend fun extractMetadata(path: String): ModelMetadata?

    /**
     * Check if a model is currently loaded.
     */
    val isModelLoaded: Boolean

    /**
     * Discover GGUF model files available on the filesystem.
     *
     * @return List of discovered models with path, name, and size
     */
    suspend fun discoverModels(): List<DiscoveredModel>
}
