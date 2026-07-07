package sk.ainet.samples.kmp.tinytransformer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Tiny Transformer (KI-ENNA)",
    ) {
        App()
    }
}
