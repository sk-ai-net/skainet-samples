@file:OptIn(ExperimentalWasmJsInterop::class)

package sk.ainet.apps.kllama.chat.data.file

import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.Buffer
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlinx.browser.document
import kotlin.coroutines.resume

@JsFun("(ab) => new Uint8Array(ab)")
private external fun createUint8View(ab: JsAny): JsAny

@JsFun("(arr) => arr.length")
private external fun uint8Length(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]")
private external fun uint8Get(arr: JsAny, i: Int): Int

private fun jsArrayBufferToByteArray(ab: JsAny): ByteArray {
    val view = createUint8View(ab)
    val length = uint8Length(view)
    return ByteArray(length) { uint8Get(view, it).toByte() }
}

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
                    val result = reader.result
                    if (result != null) {
                        val bytes = jsArrayBufferToByteArray(result)

                        cont.resume(FilePickerResult(
                            path = file.name,
                            name = file.name,
                            sizeBytes = bytes.size.toLong(),
                            sourceProvider = { Buffer().also { buf -> buf.write(bytes) } }
                        ))
                    } else {
                        cont.resume(null)
                    }
                }
                reader.onerror = { _ ->
                    cont.resume(null)
                }
                reader.readAsArrayBuffer(file.unsafeCast<org.w3c.files.Blob>())
            } else {
                cont.resume(null)
            }
        }

        input.click()
    }
}
