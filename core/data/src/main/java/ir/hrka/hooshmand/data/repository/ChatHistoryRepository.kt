package ir.hrka.hooshmand.data.repository

import ir.hrka.hooshmand.model.ChatMessage
import ir.hrka.hooshmand.model.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * Repository for locally persisted AI chat conversations and messages.
 */
interface ChatHistoryRepository {

    /**
     * Observes all conversations, newest activity first.
     */
    fun observeConversations(): Flow<List<Conversation>>

    /**
     * Observes a single conversation, or emits `null` when missing.
     *
     * @param id Conversation primary key.
     */
    fun observeConversation(id: String): Flow<Conversation?>

    /**
     * Inserts or replaces a conversation row.
     *
     * @param conversation Domain conversation to persist.
     */
    suspend fun upsertConversation(conversation: Conversation)

    /**
     * Deletes a conversation and its messages (cascade).
     *
     * @param id Conversation primary key.
     */
    suspend fun deleteConversation(id: String)

    /**
     * Observes messages for a conversation in chronological order.
     *
     * @param conversationId Parent conversation id.
     */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /**
     * Inserts or replaces a single message.
     *
     * @param message Domain message to persist.
     */
    suspend fun upsertMessage(message: ChatMessage)

    /**
     * Inserts or replaces multiple messages.
     *
     * @param messages Domain messages to persist.
     */
    suspend fun upsertMessages(messages: List<ChatMessage>)

    /**
     * Deletes a single message by id.
     *
     * @param id Message primary key.
     */
    suspend fun deleteMessage(id: String)

    /**
     * Deletes all messages in a conversation.
     *
     * @param conversationId Parent conversation id.
     */
    suspend fun deleteMessagesForConversation(conversationId: String)
}
