package sk.ainet.apps.kllama.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sk.ainet.apps.kllama.chat.data.file.FilePickerResult
import sk.ainet.apps.kllama.chat.data.model.ModelFormatDetector
import sk.ainet.apps.kllama.chat.data.repository.CommonModelLoader
import sk.ainet.apps.kllama.chat.di.ServiceLocator
import sk.ainet.apps.kllama.chat.domain.model.ChatMessage
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel
import sk.ainet.apps.kllama.chat.domain.model.GenerationState
import sk.ainet.apps.kllama.chat.domain.model.InferenceConfig
import sk.ainet.apps.kllama.chat.domain.model.InferenceStatistics
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.model.MessageRole
import sk.ainet.apps.kllama.chat.domain.model.ModelDiscoveryState
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.ModelLoadingState
import sk.ainet.apps.kllama.chat.domain.model.currentTimeMillis
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult
import sk.ainet.apps.kllama.chat.domain.port.ModelRepository
import sk.ainet.apps.kllama.chat.logging.AppLogger
import sk.ainet.apps.kllama.chat.logging.LogEntry

/**
 * UI state for the chat screen using sealed state hierarchies.
 */
data class ChatUiState(
    val session: ChatSession = ChatSession(),
    val modelState: ModelLoadingState = ModelLoadingState.Idle,
    val generationState: GenerationState = GenerationState.Idle,
    val discoveryState: ModelDiscoveryState = ModelDiscoveryState.Idle,
    val showModelPicker: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the chat screen.
 * Manages chat state, model loading, and inference.
 */
class ChatViewModel(
    private val modelRepository: ModelRepository,
    private val inferenceEngineFactory: (LoadedModel?) -> InferenceEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = AppLogger.logs

    private var currentInferenceEngine: InferenceEngine? = null
    private var generationJob: Job? = null

    init {
        discoverModels()
        loadEmbeddedModelIfAvailable()
    }

    /**
     * Auto-discover GGUF models in the working directory.
     */
    private fun discoverModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(discoveryState = ModelDiscoveryState.Scanning) }
            try {
                val models = modelRepository.discoverModels()
                _uiState.update {
                    it.copy(discoveryState = ModelDiscoveryState.Found(models))
                }
                // Lazily enrich models with metadata
                enrichDiscoveredModelsMetadata(models)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(discoveryState = ModelDiscoveryState.Error(
                        e.message ?: "Discovery failed"
                    ))
                }
            }
        }
    }

    /**
     * Progressively enrich discovered models with parsed metadata.
     */
    private suspend fun enrichDiscoveredModelsMetadata(models: List<DiscoveredModel>) {
        for (model in models) {
            try {
                val metadata = modelRepository.extractMetadata(model.path) ?: continue
                _uiState.update { state ->
                    val discovery = state.discoveryState
                    if (discovery is ModelDiscoveryState.Found) {
                        val updated = discovery.models.map { m ->
                            if (m.path == model.path) m.copy(metadata = metadata) else m
                        }
                        state.copy(discoveryState = ModelDiscoveryState.Found(updated))
                    } else {
                        state
                    }
                }
            } catch (_: Exception) {
                // Skip metadata enrichment for this model
            }
        }
    }

    /**
     * Attempt to auto-load the embedded TinyLlama model from Compose resources.
     * This provides a working chat experience immediately after app launch.
     * If no embedded model is bundled, this silently does nothing.
     */
    @OptIn(ExperimentalResourceApi::class)
    private fun loadEmbeddedModelIfAvailable() {
        viewModelScope.launch {
            try {
                val name = "tinyllama-1.1b-chat-v1.0-q4_k_m.gguf"
                val loadStartTime = currentTimeMillis()
                _uiState.update {
                    it.copy(
                        modelState = ModelLoadingState.ParsingMetadata(name),
                        errorMessage = null
                    )
                }
                // delay() yields to the browser event loop on WASM (via setTimeout),
                // unlike yield() which only yields to other coroutines on the same dispatcher.
                delay(50)
                AppLogger.info("ChatViewModel", "Checkpoint: UI state set", mapOf(
                    "elapsedMs" to "${currentTimeMillis() - loadStartTime}"
                ))

                AppLogger.info("ChatViewModel", "Downloading embedded model: $name")
                val bytes = kllamademo.composeapp.generated.resources.Res.readBytes(
                    "files/$name"
                )
                AppLogger.info(
                    "ChatViewModel",
                    "Embedded model downloaded: ${bytes.size / 1024 / 1024} MB",
                    mapOf("elapsedMs" to "${currentTimeMillis() - loadStartTime}")
                )

                _uiState.update {
                    it.copy(modelState = ModelLoadingState.LoadingWeights(name))
                }
                delay(50)
                AppLogger.info("ChatViewModel", "Checkpoint: LoadingWeights emitted", mapOf(
                    "elapsedMs" to "${currentTimeMillis() - loadStartTime}"
                ))

                val source = Buffer().also { it.write(bytes) }

                _uiState.update {
                    it.copy(
                        modelState = ModelLoadingState.LoadingWeights(
                            name, phase = "Parsing weights — this may take a moment"
                        )
                    )
                }
                delay(50)
                AppLogger.info("ChatViewModel", "Checkpoint: about to call loadModel", mapOf(
                    "elapsedMs" to "${currentTimeMillis() - loadStartTime}"
                ))

                when (val result = modelRepository.loadModel(
                    source = source,
                    name = name,
                    sizeBytes = bytes.size.toLong(),
                    format = ModelFormat.GGUF
                )) {
                    is ModelLoadResult.Success -> {
                        currentInferenceEngine = inferenceEngineFactory(result.model)
                        _uiState.update {
                            it.copy(
                                modelState = ModelLoadingState.Loaded(result.model, 0),
                                showModelPicker = false
                            )
                        }
                        AppLogger.info(
                            "ChatViewModel",
                            "Embedded model loaded: $name"
                        )
                    }
                    is ModelLoadResult.Error -> {
                        AppLogger.debug(
                            "ChatViewModel",
                            "Embedded model load failed: ${result.message}"
                        )
                        _uiState.update { it.copy(modelState = ModelLoadingState.Idle) }
                    }
                }
            } catch (_: Exception) {
                // No embedded model resource available — stay idle
                _uiState.update { state ->
                    if (state.modelState is ModelLoadingState.LoadingWeights ||
                        state.modelState is ModelLoadingState.ParsingMetadata) {
                        state.copy(modelState = ModelLoadingState.Idle)
                    } else {
                        state
                    }
                }
            }
        }
    }

    /**
     * Load a discovered model by path with phased progress.
     */
    fun loadDiscoveredModel(model: DiscoveredModel) {
        val loader = modelRepository as? sk.ainet.apps.kllama.chat.data.repository.ModelRepositoryImpl
        val platformLoader = loader?.let {
            try {
                // Access the platform loader to get CommonModelLoader for progress flow
                sk.ainet.apps.kllama.chat.di.ServiceLocator.getPlatformLoader()
            } catch (_: Exception) { null }
        }

        if (platformLoader is CommonModelLoader) {
            viewModelScope.launch {
                _uiState.update { it.copy(errorMessage = null) }
                platformLoader.loadModelWithProgress(model.path).collect { state ->
                    _uiState.update { it.copy(modelState = state) }
                    if (state is ModelLoadingState.Loaded) {
                        currentInferenceEngine = inferenceEngineFactory(state.model)
                        _uiState.update { it.copy(showModelPicker = false) }
                    } else if (state is ModelLoadingState.Error) {
                        _uiState.update { it.copy(errorMessage = state.message) }
                    }
                }
            }
        } else {
            // Fallback to standard loading
            loadModel(model.path)
        }
    }

    /**
     * Load a model from the given path.
     */
    fun loadModel(path: String) {
        val platformLoader = try {
            sk.ainet.apps.kllama.chat.di.ServiceLocator.getPlatformLoader()
        } catch (_: Exception) { null }

        if (platformLoader is CommonModelLoader) {
            viewModelScope.launch {
                _uiState.update { it.copy(errorMessage = null) }
                platformLoader.loadModelWithProgress(path).collect { state ->
                    _uiState.update { it.copy(modelState = state) }
                    if (state is ModelLoadingState.Loaded) {
                        currentInferenceEngine = inferenceEngineFactory(state.model)
                        _uiState.update { it.copy(showModelPicker = false) }
                    } else if (state is ModelLoadingState.Error) {
                        _uiState.update { it.copy(errorMessage = state.message) }
                    }
                }
            }
        } else {
            // Fallback to non-progress loading
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        modelState = ModelLoadingState.LoadingWeights(
                            path.substringAfterLast('/').substringAfterLast('\\')
                        ),
                        errorMessage = null
                    )
                }

                when (val result = modelRepository.loadModel(path)) {
                    is ModelLoadResult.Success -> {
                        currentInferenceEngine = inferenceEngineFactory(result.model)
                        _uiState.update {
                            it.copy(
                                modelState = ModelLoadingState.Loaded(result.model, 0),
                                showModelPicker = false
                            )
                        }
                    }
                    is ModelLoadResult.Error -> {
                        _uiState.update {
                            it.copy(
                                modelState = ModelLoadingState.Error(result.message),
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Load a model from a [FilePickerResult].
     * Uses source-based loading when a sourceProvider is available (Web, Android, iOS),
     * falls back to path-based loading otherwise (JVM Desktop).
     */
    fun loadModel(fileResult: FilePickerResult) {
        val sourceProvider = fileResult.sourceProvider
        if (sourceProvider != null) {
            val format = ModelFormatDetector.detectFromPath(fileResult.name)
            viewModelScope.launch {
                val fileName = fileResult.name
                _uiState.update {
                    it.copy(
                        modelState = ModelLoadingState.LoadingWeights(fileName),
                        errorMessage = null
                    )
                }

                val source = sourceProvider()
                when (val result = modelRepository.loadModel(source, fileResult.name, fileResult.sizeBytes, format)) {
                    is ModelLoadResult.Success -> {
                        currentInferenceEngine = inferenceEngineFactory(result.model)
                        _uiState.update {
                            it.copy(
                                modelState = ModelLoadingState.Loaded(result.model, 0),
                                showModelPicker = false
                            )
                        }
                    }
                    is ModelLoadResult.Error -> {
                        _uiState.update {
                            it.copy(
                                modelState = ModelLoadingState.Error(result.message),
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
        } else {
            loadModel(fileResult.path)
        }
    }

    /**
     * Unload the current model.
     */
    fun unloadModel() {
        stopGeneration()
        modelRepository.unloadModel()
        currentInferenceEngine = null
        _uiState.update {
            it.copy(
                modelState = ModelLoadingState.Idle,
                session = ChatSession()
            )
        }
    }

    /**
     * Send a user message and generate a response.
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = content.trim()
        )

        // Add user message to session
        _uiState.update {
            it.copy(
                session = it.session.addMessage(userMessage),
                errorMessage = null
            )
        }

        // Start generating response
        generateResponse()
    }

    /**
     * Generate an assistant response for the current session.
     */
    private fun generateResponse() {
        val engine = currentInferenceEngine ?: run {
            // Use demo mode if no model loaded
            currentInferenceEngine = inferenceEngineFactory(null)
            currentInferenceEngine
        }

        if (engine == null) {
            _uiState.update { it.copy(errorMessage = "No inference engine available") }
            return
        }

        // Add placeholder assistant message
        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                session = it.session.addMessage(assistantMessage),
                generationState = GenerationState.Generating("", InferenceStatistics())
            )
        }

        generationJob = viewModelScope.launch {
            val responseBuilder = StringBuilder()

            try {
                engine.generate(
                    session = _uiState.value.session,
                    config = InferenceConfig()
                ).flowOn(Dispatchers.Default)
                    .conflate()
                    .collect { token ->
                    responseBuilder.append(token.token)
                    val response = responseBuilder.toString()

                    _uiState.update { state ->
                        state.copy(
                            session = state.session.updateLastMessage(
                                content = response,
                                isStreaming = true,
                                tokenCount = token.statistics.tokensGenerated
                            ),
                            generationState = GenerationState.Generating(response, token.statistics)
                        )
                    }
                }

                // Finalize the message
                val finalResponse = responseBuilder.toString()
                _uiState.update { state ->
                    val stats = (state.generationState as? GenerationState.Generating)?.statistics
                        ?: InferenceStatistics()
                    state.copy(
                        session = state.session.updateLastMessage(
                            content = finalResponse,
                            isStreaming = false,
                            tokenCount = stats.tokensGenerated
                        ),
                        generationState = GenerationState.Complete(finalResponse, stats)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        generationState = GenerationState.Error("Generation error: ${e.message}"),
                        errorMessage = "Generation error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Stop the current generation.
     */
    fun stopGeneration() {
        currentInferenceEngine?.stopGeneration()
        generationJob?.cancel()
        generationJob = null

        _uiState.update { state ->
            val genState = state.generationState
            if (genState is GenerationState.Generating) {
                state.copy(
                    session = state.session.updateLastMessage(
                        content = genState.currentResponse,
                        isStreaming = false
                    ),
                    generationState = GenerationState.Complete(
                        genState.currentResponse,
                        genState.statistics
                    )
                )
            } else {
                state
            }
        }
    }

    /**
     * Update the system prompt for the current session.
     */
    fun updateSystemPrompt(prompt: String) {
        _uiState.update {
            it.copy(session = it.session.copy(systemPrompt = prompt))
        }
    }

    /**
     * Clear the chat history.
     */
    fun clearChat() {
        stopGeneration()
        _uiState.update {
            it.copy(
                session = it.session.clearMessages(),
                generationState = GenerationState.Idle
            )
        }
    }

    /**
     * Toggle the model picker visibility.
     */
    fun toggleModelPicker() {
        _uiState.update { it.copy(showModelPicker = !it.showModelPicker) }
    }

    /**
     * Dismiss any error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopGeneration()
    }
}
