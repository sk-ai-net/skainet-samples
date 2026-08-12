package sk.ainet.samples.kernelrace

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kernelrace.composeapp.generated.resources.Res
import kotlinx.browser.document
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.samples.kernelrace.engine.LlmEngine
import sk.ainet.samples.kernelrace.model.ModelData
import sk.ainet.samples.kernelrace.vm.ChatViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        val viewModel = remember {
            ChatViewModel(loadModel = { onProgress ->
                onProgress("Downloading model bundle…")
                val bytes = Res.readBytes("files/SmolLM2-135M-Instruct-Q8_0.gguf")
                onProgress("Building runtime…")
                LlmEngine.load(DirectCpuExecutionContext(), ModelData.Bytes(bytes))
            })
        }
        App(viewModel = viewModel, skainetVersion = SKAINET_VERSION)
    }
}
