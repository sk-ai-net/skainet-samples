package sk.ainet.samples.kmp.tinytransformer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import sk.ainet.samples.kmp.tinytransformer.i18n.Language
import sk.ainet.samples.kmp.tinytransformer.i18n.stringsFor
import sk.ainet.ui.theme.SKaiNETTheme
import sk.ainet.ui.theme.ThemeController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val viewModel = remember { TinyTransformerViewModel() }
    val state by viewModel.uiState.collectAsState()
    val strings = stringsFor(state.language)

    // Theme toggle is only shown on the web (like the other SKaiNET samples).
    val themeController = remember { if (isWasmPlatform) ThemeController() else null }

    SKaiNETTheme(themeController = themeController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.appTitle) },
                    actions = {
                        TextButton(onClick = viewModel::toggleLanguage) {
                            Text(if (state.language == Language.EN) "DE" else "EN")
                        }
                        if (themeController != null) {
                            IconButton(onClick = { themeController.toggleTheme() }) {
                                Text(if (themeController.isDarkTheme) "☀️" else "🌙")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            TinyTransformerScreen(
                viewModel = viewModel,
                state = state,
                strings = strings,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
