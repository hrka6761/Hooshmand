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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ir.hrka.hooshmand.ai_chat.impl.AiChatMessage
import ir.hrka.hooshmand.ai_chat.impl.AiChatMessageRole
import ir.hrka.hooshmand.ai_chat.impl.R
import ir.hrka.hooshmand.ai_chat.impl.speech.AiChatListenResult
import ir.hrka.hooshmand.ai_chat.impl.speech.AiChatSpeakResult
import ir.hrka.hooshmand.ai_chat.impl.speech.AiChatSpeechToText
import ir.hrka.hooshmand.ai_chat.impl.speech.AiChatTextToSpeech
import kotlinx.coroutines.launch

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
 * @param focusRequester Optional [FocusRequester] for programmatic focus control of the input field.
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
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val listState = rememberLazyListState()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messageCopiedLabel = stringResource(R.string.ai_chat_message_copied)
    val ttsEmptyText = stringResource(R.string.ai_chat_tts_empty_text)
    val ttsSpeakFailed = stringResource(R.string.ai_chat_tts_speak_failed)
    val ttsUnavailableFa = stringResource(R.string.ai_chat_tts_language_unavailable_fa)
    val ttsUnavailableAr = stringResource(R.string.ai_chat_tts_language_unavailable_ar)
    val ttsUnavailableEn = stringResource(R.string.ai_chat_tts_language_unavailable_en)
    val sttNotAvailable = stringResource(R.string.ai_chat_stt_not_available)
    val sttPermissionDenied = stringResource(R.string.ai_chat_stt_permission_denied)
    val sttNetworkOrOffline = stringResource(R.string.ai_chat_stt_network_or_offline)
    val sttNoMatch = stringResource(R.string.ai_chat_stt_no_match)
    val sttBusy = stringResource(R.string.ai_chat_stt_busy)
    val sttAudioError = stringResource(R.string.ai_chat_stt_audio_error)
    val sttError = stringResource(R.string.ai_chat_stt_error)
    val tts = remember(context) { AiChatTextToSpeech(context) }
    val speechToText = remember(context) { AiChatSpeechToText(context) }
    val speakingMessageId by tts.speakingMessageId.collectAsState()
    val preparingMessageId by tts.preparingMessageId.collectAsState()
    val isListening by speechToText.isListening.collectAsState()

    fun speakResultMessage(result: AiChatSpeakResult): String =
        when (result) {
            AiChatSpeakResult.Ok -> ""
            AiChatSpeakResult.EmptyText -> ttsEmptyText
            AiChatSpeakResult.SpeakFailed -> ttsSpeakFailed
            is AiChatSpeakResult.LanguageUnavailable ->
                when {
                    result.languageTag.startsWith("fa") -> ttsUnavailableFa
                    result.languageTag.startsWith("ar") -> ttsUnavailableAr
                    else -> ttsUnavailableEn
                }
        }

    fun listenErrorMessage(hint: AiChatListenResult.ErrorHint): String =
        when (hint) {
            AiChatListenResult.ErrorHint.Permission -> sttPermissionDenied
            AiChatListenResult.ErrorHint.NetworkOrOffline -> sttNetworkOrOffline
            AiChatListenResult.ErrorHint.NoMatch -> sttNoMatch
            AiChatListenResult.ErrorHint.Busy -> sttBusy
            AiChatListenResult.ErrorHint.Audio -> sttAudioError
            AiChatListenResult.ErrorHint.Client,
            AiChatListenResult.ErrorHint.Server,
            AiChatListenResult.ErrorHint.Unknown,
            -> sttError
        }

    fun startVoiceInput() {
        if (!speechToText.isAvailable()) {
            scope.launch { snackbarHostState.showSnackbar(sttNotAvailable) }
            return
        }
        tts.stop()
        // Always replace whatever is in the field with the new voice transcript.
        onInputTextChanged("")
        val result =
            speechToText.start(
                onPartial = { partial ->
                    onInputTextChanged(partial.trim())
                },
                onFinal = { finalText ->
                    onInputTextChanged(finalText.trim())
                },
                onError = { hint ->
                    scope.launch {
                        snackbarHostState.showSnackbar(listenErrorMessage(hint))
                    }
                },
            )
        when (result) {
            AiChatListenResult.NotAvailable ->
                scope.launch { snackbarHostState.showSnackbar(sttNotAvailable) }

            AiChatListenResult.Busy ->
                scope.launch { snackbarHostState.showSnackbar(sttBusy) }

            else -> Unit
        }
    }

    val micPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                startVoiceInput()
            } else {
                scope.launch { snackbarHostState.showSnackbar(sttPermissionDenied) }
            }
        }

    fun onMicClick() {
        if (isListening) {
            speechToText.stop()
            return
        }
        val hasMicPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        if (hasMicPermission) {
            startVoiceInput()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(tts, speechToText) {
        onDispose {
            tts.shutdown()
            speechToText.destroy()
        }
    }

    // reverseLayout pins index 0 to the visual bottom. When the user sends a new message while
    // scrolled up in history, animate back to the newest item (index 0).
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Stop playback / listening when generation starts.
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            tts.stop()
            speechToText.stop()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        // Newest message at index 0 → visual bottom. Opening a conversation from
                        // history therefore starts at the latest messages with no manual scroll.
                        // Streaming growth expands upward and stays pinned to the bottom.
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = true,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = messages.asReversed(),
                                key = { it.id },
                            ) { message ->
                                AiChatMessageBubble(
                                    message = message,
                                    isSpeaking = speakingMessageId == message.id,
                                    isPreparingSpeech = preparingMessageId == message.id,
                                    onCopyMessage = {
                                        val text = message.text
                                        if (text.isBlank()) return@AiChatMessageBubble
                                        scope.launch {
                                            clipboard.setClipEntry(
                                                ClipEntry(
                                                    ClipData.newPlainText("ai_chat_message", text),
                                                ),
                                            )
                                            snackbarHostState.showSnackbar(messageCopiedLabel)
                                        }
                                    },
                                    onSpeakMessage = {
                                        val result =
                                            tts.speakOrStop(
                                                messageId = message.id,
                                                text = message.text,
                                            )
                                        if (result !is AiChatSpeakResult.Ok) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    speakResultMessage(result),
                                                )
                                            }
                                        }
                                    },
                                )
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
                isListening = isListening,
                enabled = !isModelInitializing,
                onInputTextChanged = onInputTextChanged,
                onSendMessage = onSendMessage,
                onStopGeneration = onStopGeneration,
                onMicClick = ::onMicClick,
                focusRequester = focusRequester,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
        )
    }
}

/**
 * One chat bubble aligned by [AiChatMessage.role], matching Gallery user/agent placement.
 *
 * @param message Message content and role to render.
 * @param isSpeaking `true` when this message is currently being read aloud.
 * @param isPreparingSpeech `true` while the voice engine is loading for this message.
 * @param onCopyMessage Called when the user taps the copy action for this message.
 * @param onSpeakMessage Called when the user taps the speak action for a finished model reply.
 */
@Composable
private fun AiChatMessageBubble(
    message: AiChatMessage,
    isSpeaking: Boolean,
    isPreparingSpeech: Boolean,
    onCopyMessage: () -> Unit,
    onSpeakMessage: () -> Unit,
) {
    val isUser = message.role == AiChatMessageRole.User
    val isError = message.role == AiChatMessageRole.Error
    val isModel = message.role == AiChatMessageRole.Model
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
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
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        }
    val canCopy = message.text.isNotBlank()
    val canSpeak = isModel && !message.isStreaming && message.text.isNotBlank()

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
                AiChatDirectionalContent(text = message.text) {
                    val isRtl = resolvesToRtl(message.text)
                    when (message.role) {
                        AiChatMessageRole.Model -> {
                            AiChatMarkdownText(
                                markdown = message.text,
                                color = contentColor,
                            )
                        }

                        AiChatMessageRole.User,
                        AiChatMessageRole.Error,
                            -> {
                            Text(
                                text = message.text.ifBlank { " " },
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        textDirection =
                                            if (isRtl) {
                                                TextDirection.Rtl
                                            } else {
                                                TextDirection.Ltr
                                            },
                                    ),
                                color = contentColor,
                                textAlign = if (isRtl) TextAlign.Right else TextAlign.Left,
                            )
                        }
                    }
                }
                if (message.isStreaming) {
                    Text(
                        text = stringResource(R.string.ai_chat_streaming_indicator),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                    )
                }
            }

            if (canCopy || canSpeak) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canCopy) {
                        IconButton(
                            onClick = onCopyMessage,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription =
                                    stringResource(R.string.ai_chat_copy_message_cd),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (canSpeak) {
                        val preparingSpeechCd =
                            stringResource(R.string.ai_chat_tts_preparing_cd)
                        IconButton(
                            onClick = onSpeakMessage,
                            modifier = Modifier.size(32.dp),
                        ) {
                            when {
                                isPreparingSpeech -> {
                                    CircularProgressIndicator(
                                        modifier =
                                            Modifier
                                                .size(18.dp)
                                                .semantics {
                                                    contentDescription = preparingSpeechCd
                                                },
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                isSpeaking -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription =
                                            stringResource(R.string.ai_chat_stop_speaking_cd),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                else -> {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeUp,
                                        contentDescription =
                                            stringResource(R.string.ai_chat_speak_message_cd),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom input row with a bordered text field, optional voice input, and Send or Stop action.
 *
 * @param inputText Current draft text.
 * @param isGenerating When `true`, shows Stop instead of Send.
 * @param isListening When `true`, the mic button shows an active listening state.
 * @param enabled When `false`, disables typing and actions (e.g. while the model loads).
 * @param onInputTextChanged Called on text edits.
 * @param onSendMessage Called when Send is tapped.
 * @param onStopGeneration Called when Stop is tapped.
 * @param onMicClick Called when the voice-input mic is tapped.
 * @param focusRequester Passed to the [TextField] to manage focus.
 */
@Composable
private fun AiChatMessageInput(
    inputText: String,
    isGenerating: Boolean,
    isListening: Boolean,
    enabled: Boolean,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStopGeneration: () -> Unit,
    onMicClick: () -> Unit,
    focusRequester: FocusRequester,
) {
    val canSend = enabled && !isGenerating && !isListening && inputText.isNotBlank()
    val canUseMic = enabled && !isGenerating

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isListening) {
            Text(
                text = stringResource(R.string.ai_chat_voice_input_listening),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .focusRequester(focusRequester)
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

            IconButton(
                onClick = onMicClick,
                enabled = canUseMic,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor =
                            if (isListening) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        contentColor =
                            if (isListening) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape),
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription =
                        stringResource(
                            if (isListening) {
                                R.string.ai_chat_voice_input_stop_cd
                            } else {
                                R.string.ai_chat_voice_input_cd
                            },
                        ),
                )
            }

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
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Initializing")
@Composable
private fun AiChatPanelInitializingPreview() {
    MaterialTheme {
        AiChatPanel(
            messages = emptyList(),
            inputText = "",
            isGenerating = false,
            isModelInitializing = true,
            runtimeErrorMessage = null,
            onInputTextChanged = {},
            onSendMessage = {},
            onStopGeneration = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Empty Hint")
@Composable
private fun AiChatPanelEmptyPreview() {
    MaterialTheme {
        AiChatPanel(
            messages = emptyList(),
            inputText = "",
            isGenerating = false,
            isModelInitializing = false,
            runtimeErrorMessage = null,
            onInputTextChanged = {},
            onSendMessage = {},
            onStopGeneration = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Conversation")
@Composable
private fun AiChatPanelConversationPreview() {
    MaterialTheme {
        AiChatPanel(
            messages =
                listOf(
                    AiChatMessage("1", AiChatMessageRole.User, "Hello!"),
                    AiChatMessage(
                        "2",
                        AiChatMessageRole.Model,
                        "Hi there! How can I help you today?"
                    ),
                    AiChatMessage("3", AiChatMessageRole.User, "Tell me about on-device AI."),
                ),
            inputText = "What are the benefits?",
            isGenerating = false,
            isModelInitializing = false,
            runtimeErrorMessage = null,
            onInputTextChanged = {},
            onSendMessage = {},
            onStopGeneration = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Generating")
@Composable
private fun AiChatPanelGeneratingPreview() {
    MaterialTheme {
        AiChatPanel(
            messages =
                listOf(
                    AiChatMessage("1", AiChatMessageRole.User, "Write a poem."),
                    AiChatMessage(
                        "2",
                        AiChatMessageRole.Model,
                        "In silicon dreams, where logic flows,\nA spark of thought in circuits grows...",
                        isStreaming = true,
                    ),
                ),
            inputText = "",
            isGenerating = true,
            isModelInitializing = false,
            runtimeErrorMessage = null,
            onInputTextChanged = {},
            onSendMessage = {},
            onStopGeneration = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Runtime Error")
@Composable
private fun AiChatPanelErrorPreview() {
    MaterialTheme {
        AiChatPanel(
            messages =
                listOf(
                    AiChatMessage("1", AiChatMessageRole.User, "Try again."),
                ),
            inputText = "Try again.",
            isGenerating = false,
            isModelInitializing = false,
            runtimeErrorMessage = "Failed to connect to the AI runtime. Please check your settings.",
            onInputTextChanged = {},
            onSendMessage = {},
            onStopGeneration = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    name = "Keyboard Focused",
    heightDp = 400, // Reduced height to simulate keyboard space
)
@Composable
private fun AiChatPanelKeyboardFocusedPreview() {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme {
        AiChatPanel(
            messages =
                listOf(
                    AiChatMessage("1", AiChatMessageRole.User, "Hello!"),
                    AiChatMessage("2", AiChatMessageRole.Model, "How can I help?"),
                ),
            inputText = "I have a question about...",
            isGenerating = false,
            isModelInitializing = false,
            runtimeErrorMessage = null,
            onInputTextChanged = {},
            onSendMessage = {},
            onStopGeneration = {},
            focusRequester = focusRequester,
        )
    }
}
