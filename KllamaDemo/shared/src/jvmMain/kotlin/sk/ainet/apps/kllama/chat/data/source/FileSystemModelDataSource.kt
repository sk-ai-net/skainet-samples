package sk.ainet.apps.kllama.chat.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sk.ainet.apps.kllama.chat.domain.model.DiscoveredModel
import sk.ainet.apps.kllama.chat.logging.AppLogger
import java.io.File

/**
 * JVM-only data source that scans the working directory for GGUF model files.
 */
class FileSystemModelDataSource : ModelDataSource {

    override suspend fun discoverModels(): List<DiscoveredModel> = withContext(Dispatchers.IO) {
        val workingDir = File(System.getProperty("user.dir"))
        AppLogger.info("FileSystemModelDataSource", "Scanning for GGUF files", mapOf(
            "directory" to workingDir.absolutePath
        ))

        val ggufFiles = workingDir.listFiles { file ->
            file.isFile && file.extension.equals("gguf", ignoreCase = true)
        } ?: emptyArray()

        val models = ggufFiles.map { file ->
            DiscoveredModel(
                path = file.absolutePath,
                fileName = file.name,
                sizeBytes = file.length()
            )
        }.sortedBy { it.fileName }

        AppLogger.info("FileSystemModelDataSource", "Scan complete", mapOf(
            "modelsFound" to "${models.size}"
        ))

        models
    }
}
