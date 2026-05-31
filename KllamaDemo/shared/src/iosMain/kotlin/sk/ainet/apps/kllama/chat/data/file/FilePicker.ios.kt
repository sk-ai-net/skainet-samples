package sk.ainet.apps.kllama.chat.data.file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.Buffer
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

actual class FilePicker {

    @OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    actual suspend fun pickFile(extensions: List<String>): FilePickerResult? = suspendCancellableCoroutine { cont ->
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeData)
        )

        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                if (url == null) {
                    cont.resume(null)
                    return
                }

                val accessing = url.startAccessingSecurityScopedResource()
                try {
                    val data = NSData.create(contentsOfURL = url)
                    if (data == null) {
                        cont.resume(null)
                        return
                    }

                    val dataLength = data.length.toInt()
                    if (dataLength == 0) {
                        cont.resume(null)
                        return
                    }

                    val bytes = ByteArray(dataLength)
                    bytes.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), data.bytes, data.length)
                    }

                    val name = url.lastPathComponent ?: "model.gguf"

                    cont.resume(
                        FilePickerResult(
                            path = url.path ?: name,
                            name = name,
                            sizeBytes = bytes.size.toLong(),
                            sourceProvider = { Buffer().also { buf -> buf.write(bytes) } }
                        )
                    )
                } finally {
                    if (accessing) {
                        url.stopAccessingSecurityScopedResource()
                    }
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                cont.resume(null)
            }
        }

        picker.delegate = delegate
        picker.allowsMultipleSelection = false

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)
    }
}
