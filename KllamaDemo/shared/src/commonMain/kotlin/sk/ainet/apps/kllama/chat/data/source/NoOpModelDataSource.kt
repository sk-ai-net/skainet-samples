package sk.ainet.apps.kllama.chat.data.source

import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel

/**
 * Fallback data source for platforms without filesystem scanning (iOS, Android, Web).
 */
class NoOpModelDataSource : ModelDataSource {
    override suspend fun discoverModels(): List<DiscoveredModel> = emptyList()
}
