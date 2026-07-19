package ir.hrka.hooshmand.ai_chat_history.impl

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hrka.hooshmand.model.Conversation

/**
 * AI chat history feature entry screen.
 *
 * @param onConversationClick Opens an existing conversation.
 * @param onNewChat Starts a new conversation.
 * @param onBack Returns to the previous destination.
 * @param modifier Optional [Modifier] for the root layout.
 * @param viewModel Screen ViewModel that observes saved conversations.
 */
@Composable
fun AiChatHistoryScreen(
    onConversationClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiChatHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var conversationPendingDelete by remember { mutableStateOf<Conversation?>(null) }

    AiChatHistoryScreenContent(
        uiState = uiState,
        onConversationClick = onConversationClick,
        onNewChat = onNewChat,
        onBack = onBack,
        onDeleteClick = { conversationPendingDelete = it },
        modifier = modifier,
    )

    conversationPendingDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationPendingDelete = null },
            title = { Text(text = stringResource(R.string.delete_conversation_title)) },
            text = { Text(text = stringResource(R.string.delete_conversation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(conversation.id)
                        conversationPendingDelete = null
                    },
                ) {
                    Text(text = stringResource(R.string.delete_conversation_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationPendingDelete = null }) {
                    Text(text = stringResource(R.string.delete_conversation_cancel))
                }
            },
        )
    }
}

/**
 * Stateless history screen content used by [AiChatHistoryScreen] and Compose previews.
 *
 * @param uiState History list state.
 * @param onConversationClick Opens an existing conversation.
 * @param onNewChat Starts a new conversation.
 * @param onBack Returns to the previous destination.
 * @param onDeleteClick Requests deletion confirmation for a conversation.
 * @param modifier Optional [Modifier] for the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiChatHistoryScreenContent(
    uiState: AiChatHistoryUiState,
    onConversationClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onBack: () -> Unit,
    onDeleteClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.top_bar_title_txt),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewChat,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(text = stringResource(R.string.new_chat_fab)) },
            )
        },
    ) { paddingValues ->
        if (uiState.conversations.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_history_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    items = uiState.conversations,
                    key = { it.id },
                ) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                        onDeleteClick = { onDeleteClick(conversation) },
                    )
                }
            }
        }
    }
}

/**
 * One conversation row in the history list.
 *
 * @param conversation Conversation to display.
 * @param onClick Opens the conversation.
 * @param onDeleteClick Requests deletion.
 * @param modifier Optional [Modifier] for the row.
 */
@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = conversation.title.ifBlank { stringResource(R.string.untitled_conversation) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Text(
                text =
                    DateUtils
                        .getRelativeTimeSpanString(
                            conversation.updatedAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        )
                        .toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete_conversation_cd),
                )
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun AiChatHistoryScreenEmptyPreview() {
    AiChatHistoryScreenContent(
        uiState = AiChatHistoryUiState(),
        onConversationClick = {},
        onNewChat = {},
        onBack = {},
        onDeleteClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun AiChatHistoryScreenPreview() {
    AiChatHistoryScreenContent(
        uiState =
            AiChatHistoryUiState(
                conversations =
                    listOf(
                        Conversation(
                            id = "1",
                            title = "Explain Kotlin coroutines",
                            createdAt = System.currentTimeMillis() - 86_400_000,
                            updatedAt = System.currentTimeMillis() - 3_600_000,
                        ),
                        Conversation(
                            id = "2",
                            title = "Persian poem ideas",
                            createdAt = System.currentTimeMillis() - 172_800_000,
                            updatedAt = System.currentTimeMillis() - 86_400_000,
                        ),
                    ),
            ),
        onConversationClick = {},
        onNewChat = {},
        onBack = {},
        onDeleteClick = {},
    )
}
