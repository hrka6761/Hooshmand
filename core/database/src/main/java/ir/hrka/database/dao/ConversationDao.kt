package ir.hrka.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ir.hrka.database.model.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [ConversationEntity] rows.
 */
@Dao
interface ConversationDao {

    /**
     * Observes all conversations, newest activity first.
     */
    @Query(
        value = """
            SELECT * FROM conversations
            ORDER BY updated_at DESC
            """,
    )
    fun observeConversations(): Flow<List<ConversationEntity>>

    /**
     * Observes a single conversation, or emits nothing when missing.
     *
     * @param id Conversation primary key.
     */
    @Query(
        value = """
            SELECT * FROM conversations
            WHERE id = :id
            """,
    )
    fun observeConversation(id: String): Flow<ConversationEntity?>

    /**
     * Inserts or replaces a conversation row.
     *
     * @param conversation Entity to persist.
     */
    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    /**
     * Deletes a conversation. Messages are removed via FK cascade.
     *
     * @param id Conversation primary key.
     */
    @Query(
        value = """
            DELETE FROM conversations
            WHERE id = :id
            """,
    )
    suspend fun deleteConversation(id: String)
}
