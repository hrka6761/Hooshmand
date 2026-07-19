package ir.hrka.hooshmand.ai_chat_history.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hrka.hooshmand.data.repository.ChatHistoryRepository
import ir.hrka.hooshmand.model.Conversation
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the AI chat history list.
 *
 * @property chatHistoryRepository Local conversation persistence.
 */
@HiltViewModel
class AiChatHistoryViewModel @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository,
) : ViewModel() {

    /** Observable list of saved conversations, newest activity first. */
    val uiState: StateFlow<AiChatHistoryUiState> =
        chatHistoryRepository
            .observeConversations()
            .map { conversations -> AiChatHistoryUiState(conversations = conversations) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AiChatHistoryUiState(),
            )

    /**
     * Deletes a conversation and its messages.
     *
     * @param conversationId Conversation primary key.
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatHistoryRepository.deleteConversation(conversationId)
        }
    }
}

/**
 * UI state for the chat history screen.
 *
 * @property conversations Saved conversations ordered by recent activity.
 */
data class AiChatHistoryUiState(
    val conversations: List<Conversation> = emptyList(),
)
