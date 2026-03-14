package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import sk.ainet.apps.kllama.chat.data.model.ModelFormatDetector
import sk.ainet.apps.kllama.chat.di.ServiceLocator
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.ModelLoadingState
import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata
import sk.ainet.apps.kllama.chat.domain.model.currentTimeMillis
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult
import sk.ainet.apps.kllama.chat.logging.AppLogger
import sk.ainet.models.llama.LlamaModelMetadata
import sk.ainet.models.llama.LlamaRuntimeWeights
import sk.ainet.models.llama.LlamaWeightLoader
import sk.ainet.models.llama.LlamaWeightMapper
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32

/**
 * Multiplatform model loader using SKaiNET's LlamaRuntime.
 *
 * Uses [SystemFileSystem] for path-based loading (JVM, Android, iOS/Native)
 * and accepts a [Source] directly for platforms without filesystem paths (Web).
 *
 * On JVM, creates a real [sk.ainet.apps.kllama.LlamaRuntime] with
 * [sk.ainet.apps.kllama.CpuAttentionBackend] and [sk.ainet.apps.kllama.GGUFTokenizer].
 */
class CommonModelLoader : PlatformModelLoader {

    private var currentRuntime: Any? = null
    private var currentTokenizer: Any? = null
    private var currentWeights: LlamaRuntimeWeights<FP32>? = null
    private var currentPath: String? = null
    private val ctx = DirectCpuExecutionContext()

    override suspend fun loadFromPath(path: String, format: ModelFormat): ModelLoadResult {
        return when (format) {
            ModelFormat.GGUF -> loadGgufFromPath(path)
            ModelFormat.SAFETENSORS -> ModelLoadResult.Error("SafeTensors loading not yet implemented")
            ModelFormat.UNKNOWN -> ModelLoadResult.Error("Unknown model format")
        }
    }

    override suspend fun loadFromSource(
        source: Source,
        name: String,
        sizeBytes: Long,
        format: ModelFormat
    ): ModelLoadResult {
        return when (format) {
            ModelFormat.GGUF -> loadGgufFromSource({ source }, name, sizeBytes)
            ModelFormat.SAFETENSORS -> ModelLoadResult.Error("SafeTensors loading not yet implemented")
            ModelFormat.UNKNOWN -> ModelLoadResult.Error("Unknown model format")
        }
    }

    override suspend fun extractMetadata(path: String, format: ModelFormat): ModelMetadata? {
        return when (format) {
            ModelFormat.GGUF -> extractGgufMetadata(path)
            ModelFormat.SAFETENSORS -> null
            ModelFormat.UNKNOWN -> null
        }
    }

    override fun unload() {
        currentRuntime = null
        currentTokenizer = null
        currentWeights = null
        currentPath = null
        ServiceLocator.setRuntime(null)
        ServiceLocator.setTokenizer(null)
    }

    /**
     * Loads a GGUF model from the given path, emitting phased [ModelLoadingState] progress.
     */
    fun loadModelWithProgress(path: String): Flow<ModelLoadingState> = flow {
        val fileName = path.substringAfterLast('/').substringAfterLast('\\')
        try {
            val ioPath = Path(path)
            if (!SystemFileSystem.exists(ioPath)) {
                emit(ModelLoadingState.Error("File not found: $path"))
                return@flow
            }

            val loadStartTime = currentTimeMillis()
            val sizeBytes = SystemFileSystem.metadataOrNull(ioPath)?.size ?: 0L
            val sourceProvider = { SystemFileSystem.source(ioPath).buffered() }

            emit(ModelLoadingState.ParsingMetadata(fileName))

            val loader = LlamaWeightLoader(
                sourceProvider = sourceProvider,
                quantPolicy = QuantPolicy.DEQUANTIZE_TO_FP32
            )

            emit(ModelLoadingState.LoadingWeights(fileName))
            AppLogger.debug("ModelLoader", "loadModelWithProgress: loadToMap starting")

            val weightStart = currentTimeMillis()
            val weights = loader.loadToMap<FP32, Float>(ctx)
            AppLogger.debug("ModelLoader", "loadModelWithProgress: loadToMap done", mapOf(
                "elapsedMs" to "${currentTimeMillis() - weightStart}"
            ))

            emit(ModelLoadingState.LoadingWeights(fileName, phase = "Mapping weights"))
            AppLogger.debug("ModelLoader", "loadModelWithProgress: LlamaWeightMapper.map starting")

            val mapStart = currentTimeMillis()
            val runtimeWeights = LlamaWeightMapper.map(weights)
            AppLogger.debug("ModelLoader", "loadModelWithProgress: LlamaWeightMapper.map done", mapOf(
                "elapsedMs" to "${currentTimeMillis() - mapStart}"
            ))

            emit(ModelLoadingState.InitializingRuntime(fileName))
            AppLogger.debug("ModelLoader", "loadModelWithProgress: createRuntimeAndTokenizer starting")

            val runtimeStart = currentTimeMillis()
            val result = createRuntimeAndTokenizer(ctx, runtimeWeights, sourceProvider)
            AppLogger.debug("ModelLoader", "loadModelWithProgress: createRuntimeAndTokenizer done", mapOf(
                "elapsedMs" to "${currentTimeMillis() - runtimeStart}"
            ))

            currentRuntime = result.runtime
            currentTokenizer = result.tokenizer
            currentWeights = runtimeWeights
            currentPath = path
            ServiceLocator.setRuntime(result.runtime)
            ServiceLocator.setTokenizer(result.tokenizer)

            val llamaMeta = runtimeWeights.metadata
            val name = fileName.substringBeforeLast('.')
            val metadata = ModelMetadata(
                name = name,
                parameterCount = estimateParamCount(llamaMeta),
                sizeBytes = sizeBytes,
                quantization = ModelFormatDetector.detectQuantization(path),
                contextLength = llamaMeta.contextLength,
                vocabSize = llamaMeta.vocabSize,
                hiddenSize = llamaMeta.embeddingLength,
                numLayers = llamaMeta.blockCount,
                numHeads = llamaMeta.headCount
            )

            val totalLoadTime = currentTimeMillis() - loadStartTime
            AppLogger.info("ModelLoader", "Model loaded successfully", mapOf(
                "name" to name,
                "totalLoadTimeMs" to "$totalLoadTime"
            ))

            val loadedModel = LoadedModel(
                metadata = metadata,
                modelPath = path,
                format = ModelFormat.GGUF
            )

            emit(ModelLoadingState.Loaded(loadedModel, totalLoadTime))
        } catch (e: Exception) {
            AppLogger.error("ModelLoader", "Load with progress failed", mapOf(
                "path" to path,
                "error" to (e.message ?: "unknown")
            ))
            emit(ModelLoadingState.Error("Failed to load GGUF file: ${e.message}", e))
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun loadGgufFromPath(path: String): ModelLoadResult = withContext(Dispatchers.Default) {
        try {
            val ioPath = Path(path)
            if (!SystemFileSystem.exists(ioPath)) {
                return@withContext ModelLoadResult.Error("File not found: $path")
            }

            val loadStartTime = currentTimeMillis()
            val sizeBytes = SystemFileSystem.metadataOrNull(ioPath)?.size ?: 0L
            val fileSizeMb = sizeBytes / 1024 / 1024
            val sourceProvider = { SystemFileSystem.source(ioPath).buffered() }

            AppLogger.info("ModelLoader", "Starting GGUF load", mapOf(
                "path" to path,
                "fileSizeMB" to "$fileSizeMb",
                "format" to "GGUF"
            ))

            val loader = LlamaWeightLoader(
                sourceProvider = sourceProvider,
                quantPolicy = QuantPolicy.DEQUANTIZE_TO_FP32
            )

            val weightStart = currentTimeMillis()
            AppLogger.debug("ModelLoader", "Loading weights...")
            val weights = loader.loadToMap<FP32, Float>(ctx)
            AppLogger.info("ModelLoader", "Weights loaded", mapOf(
                "elapsedMs" to "${currentTimeMillis() - weightStart}"
            ))

            val mapStart = currentTimeMillis()
            AppLogger.debug("ModelLoader", "Mapping weights to runtime structure...")
            val runtimeWeights = LlamaWeightMapper.map(weights)
            AppLogger.info("ModelLoader", "Weights mapped", mapOf(
                "elapsedMs" to "${currentTimeMillis() - mapStart}"
            ))

            val runtimeStart = currentTimeMillis()
            AppLogger.debug("ModelLoader", "Creating LlamaRuntime + GGUFTokenizer...")
            val result = createRuntimeAndTokenizer(ctx, runtimeWeights, sourceProvider)
            AppLogger.info("ModelLoader", "Runtime + tokenizer created", mapOf(
                "elapsedMs" to "${currentTimeMillis() - runtimeStart}"
            ))

            currentRuntime = result.runtime
            currentTokenizer = result.tokenizer
            currentWeights = runtimeWeights
            currentPath = path

            ServiceLocator.setRuntime(result.runtime)
            ServiceLocator.setTokenizer(result.tokenizer)

            val llamaMeta = runtimeWeights.metadata
            val name = path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
            val metadata = ModelMetadata(
                name = name,
                parameterCount = estimateParamCount(llamaMeta),
                sizeBytes = sizeBytes,
                quantization = ModelFormatDetector.detectQuantization(path),
                contextLength = llamaMeta.contextLength,
                vocabSize = llamaMeta.vocabSize,
                hiddenSize = llamaMeta.embeddingLength,
                numLayers = llamaMeta.blockCount,
                numHeads = llamaMeta.headCount
            )

            val totalLoadTime = currentTimeMillis() - loadStartTime
            AppLogger.info("ModelLoader", "Model loaded successfully", mapOf(
                "name" to name,
                "architecture" to llamaMeta.architecture,
                "layers" to "${llamaMeta.blockCount}",
                "contextLength" to "${llamaMeta.contextLength}",
                "vocabSize" to "${llamaMeta.vocabSize}",
                "hiddenSize" to "${llamaMeta.embeddingLength}",
                "heads" to "${llamaMeta.headCount}",
                "quantization" to metadata.quantization.name,
                "totalLoadTimeMs" to "$totalLoadTime"
            ))

            ModelLoadResult.Success(
                LoadedModel(
                    metadata = metadata,
                    modelPath = path,
                    format = ModelFormat.GGUF
                )
            )
        } catch (e: Exception) {
            AppLogger.error("ModelLoader", "Load failed", mapOf(
                "path" to path,
                "error" to (e.message ?: "unknown"),
                "errorType" to (e::class.simpleName ?: "Unknown")
            ))
            e.printStackTrace()
            ModelLoadResult.Error("Failed to load GGUF file: ${e.message}", e)
        }
    }

    private suspend fun loadGgufFromSource(
        sourceProvider: () -> Source,
        name: String,
        sizeBytes: Long
    ): ModelLoadResult = withContext(Dispatchers.Default) {
        try {
            val loadStartTime = currentTimeMillis()
            val fileSizeMb = sizeBytes / 1024 / 1024
            AppLogger.info("ModelLoader", "Starting GGUF load from source", mapOf(
                "name" to name,
                "fileSizeMB" to "$fileSizeMb",
                "format" to "GGUF"
            ))

            val loader = LlamaWeightLoader(
                sourceProvider = sourceProvider,
                quantPolicy = QuantPolicy.DEQUANTIZE_TO_FP32
            )

            val weightStart = currentTimeMillis()
            AppLogger.info("ModelLoader", "Phase 1/3: loadToMap starting...")
            val weights = loader.loadToMap<FP32, Float>(ctx)
            AppLogger.info("ModelLoader", "Phase 1/3: loadToMap done", mapOf(
                "elapsedMs" to "${currentTimeMillis() - weightStart}"
            ))
            delay(1) // yield to WASM event loop between phases

            val mapStart = currentTimeMillis()
            AppLogger.info("ModelLoader", "Phase 2/3: LlamaWeightMapper.map starting...")
            val runtimeWeights = LlamaWeightMapper.map(weights)
            AppLogger.info("ModelLoader", "Phase 2/3: LlamaWeightMapper.map done", mapOf(
                "elapsedMs" to "${currentTimeMillis() - mapStart}"
            ))
            delay(1) // yield to WASM event loop between phases

            val runtimeStart = currentTimeMillis()
            AppLogger.info("ModelLoader", "Phase 3/3: createRuntimeAndTokenizer starting...")
            val result = createRuntimeAndTokenizer(ctx, runtimeWeights, sourceProvider)
            AppLogger.info("ModelLoader", "Phase 3/3: createRuntimeAndTokenizer done", mapOf(
                "elapsedMs" to "${currentTimeMillis() - runtimeStart}"
            ))

            currentRuntime = result.runtime
            currentTokenizer = result.tokenizer
            currentWeights = runtimeWeights
            currentPath = name

            ServiceLocator.setRuntime(result.runtime)
            ServiceLocator.setTokenizer(result.tokenizer)

            val llamaMeta = runtimeWeights.metadata
            val metadata = ModelMetadata(
                name = name.substringBeforeLast("."),
                parameterCount = estimateParamCount(llamaMeta),
                sizeBytes = sizeBytes,
                quantization = ModelFormatDetector.detectQuantization(name),
                contextLength = llamaMeta.contextLength,
                vocabSize = llamaMeta.vocabSize,
                hiddenSize = llamaMeta.embeddingLength,
                numLayers = llamaMeta.blockCount,
                numHeads = llamaMeta.headCount
            )

            val totalLoadTime = currentTimeMillis() - loadStartTime
            AppLogger.info("ModelLoader", "Model loaded from source", mapOf(
                "name" to name.substringBeforeLast("."),
                "totalLoadTimeMs" to "$totalLoadTime"
            ))

            ModelLoadResult.Success(
                LoadedModel(
                    metadata = metadata,
                    modelPath = name,
                    format = ModelFormat.GGUF
                )
            )
        } catch (e: Exception) {
            AppLogger.error("ModelLoader", "Load from source failed", mapOf(
                "name" to name,
                "error" to (e.message ?: "unknown"),
                "errorType" to (e::class.simpleName ?: "Unknown")
            ))
            ModelLoadResult.Error("Failed to load GGUF from source: ${e.message}", e)
        }
    }

    private suspend fun extractGgufMetadata(path: String): ModelMetadata? {
        return try {
            val ioPath = Path(path)
            if (!SystemFileSystem.exists(ioPath)) return null

            val loader = LlamaWeightLoader(
                sourceProvider = { SystemFileSystem.source(ioPath).buffered() },
                loadTensorData = false
            )

            val weights = loader.loadToMap<FP32, Float>(ctx)
            val llamaMeta = weights.metadata
            val sizeBytes = SystemFileSystem.metadataOrNull(ioPath)?.size ?: 0L
            val name = path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')

            ModelMetadata(
                name = name,
                parameterCount = estimateParamCount(llamaMeta),
                sizeBytes = sizeBytes,
                quantization = ModelFormatDetector.detectQuantization(path),
                contextLength = llamaMeta.contextLength,
                vocabSize = llamaMeta.vocabSize,
                hiddenSize = llamaMeta.embeddingLength,
                numLayers = llamaMeta.blockCount,
                numHeads = llamaMeta.headCount
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun estimateParamCount(meta: LlamaModelMetadata): Long {
        val embedding = meta.embeddingLength.toLong() * meta.vocabSize
        val perLayer = (
            4L * meta.embeddingLength * meta.embeddingLength +
            3L * meta.embeddingLength * meta.feedForwardLength +
            2L * meta.embeddingLength
        )
        val output = meta.embeddingLength.toLong() * meta.vocabSize
        return embedding + (perLayer * meta.blockCount) + output
    }
}
