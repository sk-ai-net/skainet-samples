package sk.ainet.apps.kllama.chat.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import sk.ainet.apps.kllama.chat.domain.model.GenerationState

/**
 * Input bar for composing and sending messages.
 */
@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    generationState: GenerationState,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isGenerating = generationState is GenerationState.Generating
    var inputText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (enabled) "Type a message..." else "Load a model to start chatting"
                    )
                },
                enabled = enabled && !isGenerating,
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && enabled && !isGenerating) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                )
            )

            if (isGenerating) {
                // Stop button during generation
                FilledIconButton(
                    onClick = onStopGeneration,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = StopIcon,
                        contentDescription = "Stop generation",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            } else {
                // Send button
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && enabled,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = SendIcon,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}
