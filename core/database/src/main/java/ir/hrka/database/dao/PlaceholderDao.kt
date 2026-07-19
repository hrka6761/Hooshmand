package ir.hrka.database.dao

import androidx.room.Dao
import androidx.room.Query
import ir.hrka.database.model.PlaceholderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Temporary DAO for [PlaceholderEntity].
 *
 * Exists only so the Room scaffold exposes a public DAO surface matching Now in Android.
 * Will be replaced by chat-history DAOs later.
 */
@Dao
interface PlaceholderDao {

    /**
     * Observes all placeholder rows.
     *
     * @return Flow of placeholder entities.
     */
    @Query("SELECT * FROM placeholder")
    fun observeAll(): Flow<List<PlaceholderEntity>>
}
