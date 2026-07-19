package ir.hrka.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.hrka.database.dao.PlaceholderDao
import ir.hrka.database.model.PlaceholderEntity

/**
 * App-wide Room database.
 *
 * Kept `internal`; consumers inject DAOs via Hilt, not this class directly.
 */
@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class HooshmandDatabase : RoomDatabase() {

    /**
     * Temporary placeholder DAO until chat-history tables are added.
     */
    abstract fun placeholderDao(): PlaceholderDao
}
