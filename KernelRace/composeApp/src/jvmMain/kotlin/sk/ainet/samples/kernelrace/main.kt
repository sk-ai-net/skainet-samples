package sk.ainet.samples.kernelrace

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.samples.kernelrace.engine.LlmEngine
import sk.ainet.samples.kernelrace.model.DesktopModelProvider
import sk.ainet.samples.kernelrace.model.ModelData
import sk.ainet.samples.kernelrace.vm.ChatViewModel

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SKaiNET Kernel Race") {
        val viewModel = remember {
            ChatViewModel(loadModel = { onProgress ->
                val model = DesktopModelProvider().resolve(onProgress) as ModelData.FilePath
                onProgress("Building runtime…")
                LlmEngine.load(DirectCpuExecutionContext(), model)
            })
        }
        App(viewModel = viewModel, skainetVersion = SKAINET_VERSION)
    }
}
