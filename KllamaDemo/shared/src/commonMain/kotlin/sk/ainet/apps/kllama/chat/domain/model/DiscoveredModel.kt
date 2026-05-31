package sk.ainet.apps.kllama.chat.domain.model

/**
 * Represents a GGUF model file discovered on the filesystem.
 */
data class DiscoveredModel(
    val path: String,
    val fileName: String,
    val sizeBytes: Long,
    val metadata: ModelMetadata? = null
)

/**
 * Sealed class representing model auto-discovery phases.
 */
sealed class ModelDiscoveryState {
    data object Idle : ModelDiscoveryState()
    data object Scanning : ModelDiscoveryState()
    data class Found(val models: List<DiscoveredModel>) : ModelDiscoveryState()
    data class Error(val message: String) : ModelDiscoveryState()
}
