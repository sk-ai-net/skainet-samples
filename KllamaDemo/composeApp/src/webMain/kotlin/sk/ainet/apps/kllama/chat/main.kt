package sk.ainet.apps.kllama.chat

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import sk.ainet.apps.kllama.chat.data.repository.CommonModelLoader
import sk.ainet.apps.kllama.chat.di.ServiceLocator

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize ServiceLocator with common model loader
    if (!ServiceLocator.isInitialized) {
        ServiceLocator.configure(loader = CommonModelLoader())
    }

    ComposeViewport("composeApplication") {
        App()
    }
}
