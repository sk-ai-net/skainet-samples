package sk.ainet.apps.kllama.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.io.Buffer
import sk.ainet.apps.kllama.chat.data.file.FilePicker
import sk.ainet.apps.kllama.chat.data.file.FilePickerResult

/**
 * Composable that wires Android's ActivityResultContracts to the [FilePicker] bridge.
 * Must be called inside the Activity's setContent block.
 */
@Composable
fun SetupAndroidFilePicker() {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val deferred = FilePicker.pendingResult ?: return@rememberLauncherForActivityResult

        if (uri == null) {
            deferred.complete(null)
            return@rememberLauncherForActivityResult
        }

        try {
            val contentResolver = context.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }

            if (bytes == null) {
                deferred.complete(null)
                return@rememberLauncherForActivityResult
            }

            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "model.gguf"

            deferred.complete(
                FilePickerResult(
                    path = uri.toString(),
                    name = name,
                    sizeBytes = bytes.size.toLong(),
                    sourceProvider = { Buffer().also { buf -> buf.write(bytes) } }
                )
            )
        } catch (e: Exception) {
            deferred.complete(null)
        }
    }

    DisposableEffect(Unit) {
        FilePicker.launcher = { mimeTypes ->
            launcher.launch(mimeTypes.toTypedArray())
        }
        onDispose {
            FilePicker.launcher = null
        }
    }
}
