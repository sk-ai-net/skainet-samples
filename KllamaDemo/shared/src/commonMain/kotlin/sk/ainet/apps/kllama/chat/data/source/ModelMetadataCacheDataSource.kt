package sk.ainet.apps.kllama.chat.data.source

import sk.ainet.apps.kllama.chat.domain.model.ModelMetadata

/**
 * In-memory cache for parsed model metadata, keyed by file path.
 * Avoids re-parsing metadata for previously seen models.
 */
class ModelMetadataCacheDataSource {
    private val cache = mutableMapOf<String, ModelMetadata>()

    fun get(path: String): ModelMetadata? = cache[path]

    fun put(path: String, metadata: ModelMetadata) {
        cache[path] = metadata
    }

    fun clear() {
        cache.clear()
    }
}
