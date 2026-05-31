package sk.ainet.apps.kllama.chat.data.file

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.Buffer
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.coroutines.resume

actual class FilePicker {

    actual suspend fun pickFile(extensions: List<String>): FilePickerResult? = suspendCancellableCoroutine { cont ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = extensions.joinToString(",") { ".$it" }

        input.onchange = { _ ->
            val file = input.files?.get(0)
            if (file != null) {
                val reader = FileReader()
                reader.onload = { _ ->
                    val arrayBuffer = reader.result as ArrayBuffer
                    val int8Array = Int8Array(arrayBuffer)
                    val length = int8Array.length
                    val bytes = ByteArray(length) { i -> int8Array[i] }

                    cont.resume(FilePickerResult(
                        path = file.name,
                        name = file.name,
                        sizeBytes = bytes.size.toLong(),
                        sourceProvider = { Buffer().also { buf -> buf.write(bytes) } }
                    ))
                }
                reader.onerror = { _ ->
                    cont.resume(null)
                }
                reader.readAsArrayBuffer(file)
            } else {
                cont.resume(null)
            }
        }

        input.click()
    }
}
