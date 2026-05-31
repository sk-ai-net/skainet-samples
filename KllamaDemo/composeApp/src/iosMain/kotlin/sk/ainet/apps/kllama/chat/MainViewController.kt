package sk.ainet.apps.kllama.chat

import androidx.compose.ui.window.ComposeUIViewController
import sk.ainet.apps.kllama.chat.data.repository.CommonModelLoader
import sk.ainet.apps.kllama.chat.di.ServiceLocator

fun MainViewController() = ComposeUIViewController {
    // Initialize ServiceLocator with common model loader
    if (!ServiceLocator.isInitialized) {
        ServiceLocator.configure(loader = CommonModelLoader())
    }

    App()
}
