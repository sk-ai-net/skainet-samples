package sk.ainet.samples.kernelrace

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.samples.kernelrace.engine.LlmEngine
import sk.ainet.samples.kernelrace.model.IosModelProvider
import sk.ainet.samples.kernelrace.model.ModelData
import sk.ainet.samples.kernelrace.vm.ChatViewModel

/** Entry point called from iosApp/iOSApp.swift — same "resolve model, then load engine" shape
 *  as the JVM/Android entry points (see main.kt / MainActivity.kt). No kernelControls slot:
 *  the NEON-vs-scalar race is Android-only (see Platform.ios.kt's supportsKernelRace = false). */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val viewModel = remember {
        ChatViewModel(loadModel = { onProgress ->
            val model = IosModelProvider().resolve(onProgress) as ModelData.FilePath
            onProgress("Building runtime…")
            LlmEngine.load(DirectCpuExecutionContext(), model)
        })
    }
    App(viewModel = viewModel, skainetVersion = SKAINET_VERSION)
}
