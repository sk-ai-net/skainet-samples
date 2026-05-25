package sk.ainet.apps.kllama.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import sk.ainet.apps.kllama.chat.playground.QwenPlaygroundHost
import sk.ainet.ui.theme.SKaiNETTheme

/**
 * Main app composable with SKaiNET Design System theming. Renders the
 * Qwen3-0.6B playground (multi-tab, multi-mode showcase).
 */
@Composable
fun App(
    darkTheme: Boolean = false
) {
    SKaiNETTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            QwenPlaygroundHost()
        }
    }
}
