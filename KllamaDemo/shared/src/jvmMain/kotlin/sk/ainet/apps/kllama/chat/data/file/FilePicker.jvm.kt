package sk.ainet.apps.kllama.chat.data.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual class FilePicker {

    actual suspend fun pickFile(extensions: List<String>): FilePickerResult? = withContext(Dispatchers.IO) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Select Model File"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false

            val description = "Model files (${extensions.joinToString(", ") { "*.$it" }})"
            fileFilter = FileNameExtensionFilter(description, *extensions.toTypedArray())
        }

        val result = chooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            FilePickerResult(
                path = file.absolutePath,
                name = file.name,
                sizeBytes = file.length()
            )
        } else {
            null
        }
    }
}
