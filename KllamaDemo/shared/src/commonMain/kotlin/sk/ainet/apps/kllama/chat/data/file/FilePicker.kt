package sk.ainet.apps.kllama.chat.data.file

import kotlinx.io.Source

/**
 * Result of a file picker operation.
 */
data class FilePickerResult(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val sourceProvider: (() -> Source)? = null
)

/**
 * Platform-specific file picker interface.
 */
expect class FilePicker() {
    /**
     * Open a file picker dialog to select a model file.
     *
     * @param extensions List of allowed file extensions (without dots)
     * @return FilePickerResult if a file was selected, null if cancelled
     */
    suspend fun pickFile(extensions: List<String> = listOf("gguf", "safetensors")): FilePickerResult?
}
