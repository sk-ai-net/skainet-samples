package sk.ainet.apps.kllama.chat

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import sk.ainet.apps.kllama.chat.data.repository.CommonModelLoader
import sk.ainet.apps.kllama.chat.data.source.FileSystemModelDataSource
import sk.ainet.apps.kllama.chat.di.ServiceLocator
import sk.ainet.apps.kllama.chat.logging.AppLogger

fun main() = application {
    // Initialize ServiceLocator with common model loader and filesystem data source
    if (!ServiceLocator.isInitialized) {
        ServiceLocator.configure(
            loader = CommonModelLoader(),
            dataSource = FileSystemModelDataSource()
        )
        ServiceLocator.supportsEmbeddedModelLoading = true
    }

    logSimdStatus()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Transformer Explainer — Qwen3-0.6B on SKaiNET",
        state = rememberWindowState(size = DpSize(1024.dp, 768.dp))
    ) {
        App()
    }
}

private fun logSimdStatus() {
    val available = try {
        Class.forName("jdk.incubator.vector.FloatVector")
        true
    } catch (_: ClassNotFoundException) {
        false
    }
    AppLogger.info("Platform", "SIMD (Vector API) available: $available")
}
