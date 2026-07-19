package ir.hrka.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ir.hrka.database.model.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [MessageEntity] rows.
 */
@Dao
interface MessageDao {

    /**
     * Observes messages for a conversation in chronological order.
     *
     * @param conversationId Parent conversation id.
     */
    @Query(
        value = """
            SELECT * FROM messages
            WHERE conversation_id = :conversationId
            ORDER BY created_at ASC
            """,
    )
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Inserts or replaces a single message.
     *
     * @param message Entity to persist.
     */
    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    /**
     * Inserts or replaces multiple messages.
     *
     * @param messages Entities to persist.
     */
    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    /**
     * Deletes all messages in a conversation.
     *
     * @param conversationId Parent conversation id.
     */
    @Query(
        value = """
            DELETE FROM messages
            WHERE conversation_id = :conversationId
            """,
    )
    suspend fun deleteMessagesForConversation(conversationId: String)

    /**
     * Deletes a single message by id.
     *
     * @param id Message primary key.
     */
    @Query(
        value = """
            DELETE FROM messages
            WHERE id = :id
            """,
    )
    suspend fun deleteMessage(id: String)
}
