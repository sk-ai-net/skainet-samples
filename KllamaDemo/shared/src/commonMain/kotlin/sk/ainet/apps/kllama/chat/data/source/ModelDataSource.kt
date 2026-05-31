package sk.ainet.apps.kllama.chat.data.source

import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel

/**
 * Data source interface for discovering GGUF model files.
 */
interface ModelDataSource {
    suspend fun discoverModels(): List<DiscoveredModel>
}
