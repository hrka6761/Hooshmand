package ir.hrka.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.hrka.database.dao.ConversationDao
import ir.hrka.database.dao.MessageDao
import ir.hrka.database.model.ConversationEntity
import ir.hrka.database.model.MessageEntity

/**
 * App-wide Room database.
 *
 * Kept `internal`; consumers inject DAOs via Hilt, not this class directly.
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
internal abstract class HooshmandDatabase : RoomDatabase() {

    /**
     * DAO for conversation list and metadata.
     */
    abstract fun conversationDao(): ConversationDao

    /**
     * DAO for messages within a conversation.
     */
    abstract fun messageDao(): MessageDao
}
