package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.io.asSource
import kotlinx.io.buffered

fun main() = application {

    // Suppose we have a file to read from:
    val resourcePath = "/sinus.json"

    // Using an anonymous object to obtain a reference to a class
    val inputStream = object {}.javaClass.getResourceAsStream(resourcePath)


    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
    ) {
        inputStream?.let {
            App {
                it.asSource().buffered()
            }
        }
    }
}