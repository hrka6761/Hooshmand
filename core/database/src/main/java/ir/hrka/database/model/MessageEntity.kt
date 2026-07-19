package ir.hrka.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted chat message belonging to a [ConversationEntity].
 *
 * Mirrors the in-memory `AiChatMessage` fields that are worth storing; streaming state is not persisted.
 *
 * @property id Stable unique id (UUID string), same id used in the chat UI list.
 * @property conversationId Parent conversation id.
 * @property role Message kind: `USER`, `MODEL`, or `ERROR`.
 * @property text Message body.
 * @property createdAt Epoch millis when the message row was created.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conversation_id"]),
    ],
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    val role: String,
    val text: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
