package ir.hrka.hooshmand.data.repository

import ir.hrka.database.dao.ConversationDao
import ir.hrka.database.dao.MessageDao
import ir.hrka.hooshmand.data.model.asEntity
import ir.hrka.hooshmand.data.model.asExternalModel
import ir.hrka.hooshmand.model.ChatMessage
import ir.hrka.hooshmand.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [ChatHistoryRepository] backed by Room DAOs.
 */
@Singleton
internal class DefaultChatHistoryRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) : ChatHistoryRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeConversations().map { entities ->
            entities.map { it.asExternalModel() }
        }

    override fun observeConversation(id: String): Flow<Conversation?> =
        conversationDao.observeConversation(id).map { entity ->
            entity?.asExternalModel()
        }

    override suspend fun upsertConversation(conversation: Conversation) {
        conversationDao.upsertConversation(conversation.asEntity())
    }

    override suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversation(id)
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.observeMessages(conversationId).map { entities ->
            entities.map { it.asExternalModel() }
        }

    override suspend fun upsertMessage(message: ChatMessage) {
        messageDao.upsertMessage(message.asEntity())
    }

    override suspend fun upsertMessages(messages: List<ChatMessage>) {
        messageDao.upsertMessages(messages.map { it.asEntity() })
    }

    override suspend fun deleteMessage(id: String) {
        messageDao.deleteMessage(id)
    }

    override suspend fun deleteMessagesForConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
    }
}
