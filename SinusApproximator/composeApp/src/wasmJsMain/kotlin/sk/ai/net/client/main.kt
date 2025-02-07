package sk.ai.net.client

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import sk.ai.net.samples.kmp.sinus.approximator.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        // Suppose we have a file to read from:
        val resourcePath = "/sinus.json"
        val path: Path = Path(resourcePath)
        // Use the SystemFileSystem (the default FileSystem) to open a source.
        val source = SystemFileSystem.source(path).buffered()
        // Read the file content as a ByteString and decode it as UTF-8.
//        val content = source.readByteString().utf8()

        // Using an anonymous object to obtain a reference to a class
        //FileSystem.
        App {
            source
        }
    }

}