package ir.hrka.hooshmand.ai_chat.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.hrka.hooshmand.ai_chat.impl.AiChatMessage
import ir.hrka.hooshmand.ai_chat.impl.AiChatMessageRole
import ir.hrka.hooshmand.ai_chat.impl.R

/**
 * Gallery-style chat panel: scrollable message list plus a bottom text input with Send/Stop.
 *
 * UI-only. Wiring to [ir.hrka.llm.runtime.api.LlmRuntime] belongs in the ViewModel / screen layer.
 *
 * @param messages Ordered conversation messages to render.
 * @param inputText Current draft text in the input field.
 * @param isGenerating `true` while the model is streaming a reply (shows Stop).
 * @param isModelInitializing `true` while the runtime is loading the model.
 * @param runtimeErrorMessage Optional runtime error shown above the input.
 * @param onInputTextChanged Called when the user edits the input field.
 * @param onSendMessage Called when the user taps Send with non-blank text.
 * @param onStopGeneration Called when the user taps Stop during generation.
 * @param modifier Optional [Modifier] for the root layout.
 */
@Composable
internal fun AiChatPanel(
    messages: List<AiChatMessage>,
    inputText: String,
    isGenerating: Boolean,
    isModelInitializing: Boolean,
    runtimeErrorMessage: String?,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStopGeneration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding(),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            when {
                isModelInitializing && messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.ai_chat_model_initializing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                messages.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.ai_chat_empty_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = messages,
                            key = { it.id },
                        ) { message ->
                            AiChatMessageBubble(message = message)
                        }
                    }
                }
            }
        }

        runtimeErrorMessage?.takeIf { it.isNotBlank() }?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        AiChatMessageInput(
            inputText = inputText,
            isGenerating = isGenerating,
            enabled = !isModelInitializing,
            onInputTextChanged = onInputTextChanged,
            onSendMessage = onSendMessage,
            onStopGeneration = onStopGeneration,
        )
    }
}

/**
 * One chat bubble aligned by [AiChatMessage.role], matching Gallery user/agent placement.
 *
 * @param message Message content and role to render.
 */
@Composable
private fun AiChatMessageBubble(message: AiChatMessage) {
    val isUser = message.role == AiChatMessageRole.User
    val isError = message.role == AiChatMessageRole.Error
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor =
        when {
            isError -> MaterialTheme.colorScheme.errorContainer
            isUser -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        when {
            isError -> MaterialTheme.colorScheme.onErrorContainer
            isUser -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val shape =
        if (isUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(shape)
                    .background(backgroundColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message.text.ifBlank { " " },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
            if (message.isStreaming) {
                Text(
                    text = stringResource(R.string.ai_chat_streaming_indicator),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * Bottom input row with a bordered text field and Send or Stop action, as in Gallery.
 *
 * @param inputText Current draft text.
 * @param isGenerating When `true`, shows Stop instead of Send.
 * @param enabled When `false`, disables typing and actions (e.g. while the model loads).
 * @param onInputTextChanged Called on text edits.
 * @param onSendMessage Called when Send is tapped.
 * @param onStopGeneration Called when Stop is tapped.
 */
@Composable
private fun AiChatMessageInput(
    inputText: String,
    isGenerating: Boolean,
    enabled: Boolean,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStopGeneration: () -> Unit,
) {
    val canSend = enabled && !isGenerating && inputText.isNotBlank()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = inputText,
            onValueChange = onInputTextChanged,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp),
                    ),
            enabled = enabled && !isGenerating,
            placeholder = {
                Text(text = stringResource(R.string.ai_chat_input_placeholder))
            },
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            maxLines = 5,
        )

        if (isGenerating) {
            IconButton(
                onClick = onStopGeneration,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = stringResource(R.string.ai_chat_stop),
                )
            }
        } else {
            IconButton(
                onClick = onSendMessage,
                enabled = canSend,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.ai_chat_send),
                )
            }
        }
    }
}
