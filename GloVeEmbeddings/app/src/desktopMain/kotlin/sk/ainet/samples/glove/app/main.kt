package sk.ainet.samples.glove.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GloVe Embeddings Explorer",
    ) {
        App()
    }
}
