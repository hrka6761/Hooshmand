package ir.hrka.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Temporary Room entity required to bootstrap [ir.hrka.database.HooshmandDatabase].
 *
 * Room needs at least one entity at version 1. This placeholder will be replaced by
 * chat-history tables in a later step.
 *
 * @property id Stable primary key for the placeholder row.
 */
@Entity(tableName = "placeholder")
data class PlaceholderEntity(
    @PrimaryKey
    val id: Long,
)
