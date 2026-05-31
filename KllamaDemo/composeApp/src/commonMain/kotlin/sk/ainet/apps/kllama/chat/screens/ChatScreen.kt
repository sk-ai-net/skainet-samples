package sk.ainet.apps.kllama.chat.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.ainet.apps.kllama.chat.domain.model.GenerationState
import sk.ainet.apps.kllama.chat.domain.model.InferenceStatistics
import sk.ainet.apps.kllama.chat.domain.model.ModelLoadingState
import sk.ainet.apps.kllama.chat.ui.ChatInputBar
import sk.ainet.apps.kllama.chat.ui.ClearIcon
import sk.ainet.apps.kllama.chat.ui.MenuIcon
import sk.ainet.apps.kllama.chat.ui.MessageList
import sk.ainet.apps.kllama.chat.ui.ModelInfoCard
import sk.ainet.apps.kllama.chat.ui.StatisticsPanel
import sk.ainet.apps.kllama.chat.viewmodel.ChatUiState
import sk.ainet.apps.kllama.chat.viewmodel.ChatViewModel

/**
 * Main chat screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToModelPicker: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isGenerating = uiState.generationState is GenerationState.Generating
    val isModelLoaded = uiState.modelState is ModelLoadingState.Loaded
    val isLoadingModel = uiState.modelState is ModelLoadingState.ParsingMetadata
            || uiState.modelState is ModelLoadingState.LoadingWeights
            || uiState.modelState is ModelLoadingState.InitializingRuntime
            || uiState.modelState is ModelLoadingState.Scanning

    val statistics = when (val gs = uiState.generationState) {
        is GenerationState.Generating -> gs.statistics
        is GenerationState.Complete -> gs.statistics
        else -> InferenceStatistics()
    }

    val modelMetadata = (uiState.modelState as? ModelLoadingState.Loaded)?.model?.metadata

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChatDrawerContent(
                    uiState = uiState,
                    onLoadModel = {
                        scope.launch { drawerState.close() }
                        onNavigateToModelPicker()
                    },
                    onUnloadModel = {
                        viewModel.unloadModel()
                    },
                    onClearChat = {
                        viewModel.clearChat()
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToDiagnostics = {
                        scope.launch { drawerState.close() }
                        onNavigateToDiagnostics()
                    },
                    onUpdateSystemPrompt = { viewModel.updateSystemPrompt(it) }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = { Text("KLlama Chat") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(MenuIcon, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (uiState.session.messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearChat() }) {
                                Icon(ClearIcon, contentDescription = "Clear chat")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                ChatInputBar(
                    onSendMessage = { viewModel.sendMessage(it) },
                    onStopGeneration = { viewModel.stopGeneration() },
                    generationState = uiState.generationState,
                    enabled = true // Allow demo mode even without model
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main chat area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Model info card
                    AnimatedVisibility(
                        visible = isModelLoaded || isLoadingModel
                    ) {
                        ModelInfoCard(
                            modelState = uiState.modelState,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onClick = onNavigateToModelPicker
                        )
                    }

                    // Message list
                    Box(modifier = Modifier.weight(1f)) {
                        MessageList(
                            messages = uiState.session.messages
                        )
                    }
                }

                // Side statistics panel (visible on wider screens when generating)
                AnimatedVisibility(
                    visible = isGenerating || statistics.tokensGenerated > 0,
                    enter = slideInVertically(),
                    exit = slideOutVertically()
                ) {
                    StatisticsPanel(
                        statistics = statistics,
                        modelMetadata = modelMetadata,
                        isGenerating = isGenerating,
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Drawer content for the chat screen.
 */
@Composable
private fun ChatDrawerContent(
    uiState: ChatUiState,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit,
    onClearChat: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {},
    onUpdateSystemPrompt: (String) -> Unit = {}
) {
    val isModelLoaded = uiState.modelState is ModelLoadingState.Loaded
    var systemPromptExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "KLlama Chat",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ModelInfoCard(
            modelState = uiState.modelState,
            onClick = onLoadModel,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isModelLoaded) {
            DrawerMenuItem(
                text = "Unload Model",
                onClick = onUnloadModel
            )
        }

        if (uiState.session.messages.isNotEmpty()) {
            DrawerMenuItem(
                text = "Clear Chat",
                onClick = onClearChat
            )
        }

        DrawerMenuItem(
            text = "Load Model",
            onClick = onLoadModel
        )

        DrawerMenuItem(
            text = "Diagnostics",
            onClick = onNavigateToDiagnostics
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // System Prompt section
        androidx.compose.material3.TextButton(
            onClick = { systemPromptExpanded = !systemPromptExpanded }
        ) {
            Text(if (systemPromptExpanded) "System Prompt (collapse)" else "System Prompt (edit)")
        }

        AnimatedVisibility(visible = systemPromptExpanded) {
            OutlinedTextField(
                value = uiState.session.systemPrompt,
                onValueChange = onUpdateSystemPrompt,
                label = { Text("System Prompt") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                minLines = 3,
                maxLines = 8
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    text: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(text)
    }
}
