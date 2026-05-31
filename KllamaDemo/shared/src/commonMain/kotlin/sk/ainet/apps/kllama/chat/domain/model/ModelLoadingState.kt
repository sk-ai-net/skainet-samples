package sk.ainet.apps.kllama.chat.domain.model

/**
 * Sealed class representing phased model loading progress.
 */
sealed class ModelLoadingState {
    data object Idle : ModelLoadingState()
    data class Scanning(val filesFound: Int = 0) : ModelLoadingState()
    data class ParsingMetadata(val fileName: String) : ModelLoadingState()
    data class LoadingWeights(val fileName: String, val phase: String = "Loading weights") : ModelLoadingState()
    data class InitializingRuntime(val fileName: String) : ModelLoadingState()
    data class Loaded(val model: LoadedModel, val loadTimeMs: Long) : ModelLoadingState()
    data class Error(val message: String, val cause: Throwable? = null) : ModelLoadingState()
}
