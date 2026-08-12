package sk.ainet.samples.kernelrace

import androidx.compose.runtime.Composable
import sk.ainet.samples.kernelrace.ui.ChatScreen
import sk.ainet.samples.kernelrace.vm.ChatViewModel
import sk.ainet.ui.theme.SKaiNETTheme

@Composable
fun App(
    viewModel: ChatViewModel,
    skainetVersion: String,
    kernelControls: @Composable (busy: Boolean, viewModel: ChatViewModel, currentPrompt: () -> String) -> Unit = { _, _, _ -> },
) {
    SKaiNETTheme {
        ChatScreen(viewModel = viewModel, skainetVersion = skainetVersion, kernelControls = kernelControls)
    }
}
