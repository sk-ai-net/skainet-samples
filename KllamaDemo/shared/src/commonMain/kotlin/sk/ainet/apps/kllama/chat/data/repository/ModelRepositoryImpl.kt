package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.io.Source
import sk.ainet.apps.kllama.chat.data.model.ModelFormatDetector
import sk.ainet.apps.kllama.chat.data.source.ModelDataSource
import sk.ainet.apps.kllama.chat.data.source.ModelMetadataCacheDataSource
import sk.ainet.apps.kllama.chat.data.source.NoOpModelDataSource
import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult
import sk.ainet.apps.kllama.chat.domain.port.ModelRepository
import sk.ainet.apps.kllama.chat.logging.AppLogger

/**
 * Implementation of ModelRepository.
 * Uses platform-specific loaders for different model formats.
 */
class ModelRepositoryImpl(
    private val platformLoader: PlatformModelLoader,
    private val discoverySource: ModelDataSource = NoOpModelDataSource(),
    private val metadataCache: ModelMetadataCacheDataSource = ModelMetadataCacheDataSource()
) : ModelRepository {

    private var currentModel: LoadedModel? = null

    override suspend fun loadModel(
        path: String,
        format: ModelFormat
    ): ModelLoadResult {
        val detectedFormat = if (format == ModelFormat.UNKNOWN) {
            ModelFormatDetector.detectFromPath(path)
        } else {
            format
        }

        AppLogger.info("ModelRepository", "Format detected", mapOf(
            "path" to path,
            "format" to detectedFormat.name
        ))

        if (detectedFormat == ModelFormat.UNKNOWN) {
            AppLogger.warn("ModelRepository", "Unknown format", mapOf("path" to path))
            return ModelLoadResult.Error("Unable to detect model format from path: $path")
        }

        AppLogger.info("ModelRepository", "Delegating load to platform loader")

        return try {
            val result = platformLoader.loadFromPath(path, detectedFormat)
            if (result is ModelLoadResult.Success) {
                currentModel = result.model
                AppLogger.info("ModelRepository", "Model load succeeded", mapOf(
                    "name" to result.model.metadata.name
                ))
            } else if (result is ModelLoadResult.Error) {
                AppLogger.error("ModelRepository", "Model load failed", mapOf(
                    "error" to result.message
                ))
            }
            result
        } catch (e: Exception) {
            AppLogger.error("ModelRepository", "Model load exception", mapOf(
                "error" to (e.message ?: "unknown")
            ))
            ModelLoadResult.Error("Failed to load model: ${e.message}", e)
        }
    }

    override suspend fun loadModel(
        source: Source,
        name: String,
        sizeBytes: Long,
        format: ModelFormat
    ): ModelLoadResult {
        if (format == ModelFormat.UNKNOWN) {
            return ModelLoadResult.Error("Model format must be specified when loading from Source")
        }

        return try {
            val result = platformLoader.loadFromSource(source, name, sizeBytes, format)
            if (result is ModelLoadResult.Success) {
                currentModel = result.model
            }
            result
        } catch (e: Exception) {
            ModelLoadResult.Error("Failed to load model from source: ${e.message}", e)
        }
    }

    override fun getLoadedModel(): LoadedModel? = currentModel

    override fun unloadModel() {
        AppLogger.info("ModelRepository", "Unloading model", mapOf(
            "name" to (currentModel?.metadata?.name ?: "none")
        ))
        currentModel = null
        platformLoader.unload()
    }

    override suspend fun extractMetadata(path: String): ModelMetadata? {
        metadataCache.get(path)?.let { return it }

        val format = ModelFormatDetector.detectFromPath(path)
        if (format == ModelFormat.UNKNOWN) return null

        return try {
            val metadata = platformLoader.extractMetadata(path, format)
            if (metadata != null) {
                metadataCache.put(path, metadata)
            }
            metadata
        } catch (e: Exception) {
            null
        }
    }

    override val isModelLoaded: Boolean
        get() = currentModel != null

    override suspend fun discoverModels(): List<DiscoveredModel> {
        return try {
            discoverySource.discoverModels()
        } catch (e: Exception) {
            AppLogger.error("ModelRepository", "Model discovery failed", mapOf(
                "error" to (e.message ?: "unknown")
            ))
            emptyList()
        }
    }
}

/**
 * Platform-specific model loader interface.
 * Implementations handle the actual loading of model weights.
 */
interface PlatformModelLoader {
    suspend fun loadFromPath(path: String, format: ModelFormat): ModelLoadResult
    suspend fun loadFromSource(source: Source, name: String, sizeBytes: Long, format: ModelFormat): ModelLoadResult
    suspend fun extractMetadata(path: String, format: ModelFormat): ModelMetadata?
    fun unload()
}
