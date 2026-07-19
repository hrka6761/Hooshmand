package ir.hrka.hooshmand.data.model

import ir.hrka.database.model.ConversationEntity
import ir.hrka.hooshmand.model.Conversation

/**
 * Maps a Room [ConversationEntity] to the shared domain [Conversation] model.
 */
internal fun ConversationEntity.asExternalModel(): Conversation =
    Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

/**
 * Maps a domain [Conversation] to a Room [ConversationEntity].
 */
internal fun Conversation.asEntity(): ConversationEntity =
    ConversationEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
