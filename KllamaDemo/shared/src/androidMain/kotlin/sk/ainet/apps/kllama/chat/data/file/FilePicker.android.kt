package sk.ainet.apps.kllama.chat.data.file

import kotlinx.coroutines.CompletableDeferred

/**
 * Android file picker using a static callback bridge.
 *
 * The actual file picking is handled in the UI layer via ActivityResultContracts.
 * [SetupAndroidFilePicker] composable wires the launcher to this bridge.
 */
actual class FilePicker {

    actual suspend fun pickFile(extensions: List<String>): FilePickerResult? {
        val deferred = CompletableDeferred<FilePickerResult?>()
        pendingResult = deferred

        val mimeTypes = extensions.map { ext ->
            when (ext) {
                "gguf" -> "application/octet-stream"
                "safetensors" -> "application/octet-stream"
                else -> "application/octet-stream"
            }
        }.distinct()

        launcher?.invoke(mimeTypes)
            ?: return null // No launcher wired up

        return deferred.await()
    }

    companion object {
        /** Pending result deferred, set when pickFile is called. */
        var pendingResult: CompletableDeferred<FilePickerResult?>? = null

        /** Launcher function, wired by SetupAndroidFilePicker composable. */
        var launcher: ((List<String>) -> Unit)? = null
    }
}
