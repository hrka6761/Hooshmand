package ir.hrka.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted AI chat conversation (one history list row).
 *
 * @property id Stable unique id (UUID string).
 * @property title Display title; often derived from the first user message.
 * @property createdAt Epoch millis when the conversation was created.
 * @property updatedAt Epoch millis of the last message change; used for history sorting.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
