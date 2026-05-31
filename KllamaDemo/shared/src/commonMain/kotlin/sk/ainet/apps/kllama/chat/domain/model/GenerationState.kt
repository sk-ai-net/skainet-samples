package sk.ainet.apps.kllama.chat.domain.model

/**
 * Sealed class representing inference generation phases.
 */
sealed class GenerationState {
    data object Idle : GenerationState()
    data class Generating(val currentResponse: String, val statistics: InferenceStatistics) : GenerationState()
    data class Complete(val response: String, val statistics: InferenceStatistics) : GenerationState()
    data class Error(val message: String) : GenerationState()
}
