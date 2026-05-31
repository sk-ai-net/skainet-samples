package sk.ainet.apps.kllama.chat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import sk.ainet.apps.kllama.chat.di.ServiceLocator
import sk.ainet.apps.kllama.chat.screens.ChatScreen
import sk.ainet.apps.kllama.chat.screens.DiagnosticsScreen
import sk.ainet.apps.kllama.chat.screens.ModelSelectionScreen
import sk.ainet.apps.kllama.chat.viewmodel.ChatViewModel

/**
 * Navigation destinations for the chat app.
 */
enum class ChatDestination {
    CHAT,
    MODEL_PICKER,
    DIAGNOSTICS
}

/**
 * Main navigation host for the chat app.
 * Manages navigation between chat and model selection screens.
 */
@Composable
fun ChatNavigationHost(
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(ChatDestination.CHAT) }

    // Create view model with dependencies
    val viewModel = remember {
        ChatViewModel(
            modelRepository = ServiceLocator.getModelRepository(),
            inferenceEngineFactory = ServiceLocator.getInferenceEngineFactory()
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    when (currentDestination) {
        ChatDestination.CHAT -> {
            ChatScreen(
                viewModel = viewModel,
                onNavigateToModelPicker = {
                    currentDestination = ChatDestination.MODEL_PICKER
                },
                onNavigateToDiagnostics = {
                    currentDestination = ChatDestination.DIAGNOSTICS
                },
                modifier = modifier
            )
        }

        ChatDestination.DIAGNOSTICS -> {
            DiagnosticsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentDestination = ChatDestination.CHAT
                },
                modifier = modifier
            )
        }

        ChatDestination.MODEL_PICKER -> {
            ModelSelectionScreen(
                onModelSelected = { fileResult ->
                    viewModel.loadModel(fileResult)
                    currentDestination = ChatDestination.CHAT
                },
                onDiscoveredModelSelected = { model ->
                    viewModel.loadDiscoveredModel(model)
                    currentDestination = ChatDestination.CHAT
                },
                onNavigateBack = {
                    currentDestination = ChatDestination.CHAT
                },
                modelState = uiState.modelState,
                discoveryState = uiState.discoveryState,
                errorMessage = uiState.errorMessage,
                modifier = modifier
            )
        }
    }
}
