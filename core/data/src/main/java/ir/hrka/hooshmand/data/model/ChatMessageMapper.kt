package ir.hrka.hooshmand.data.model

import ir.hrka.database.model.MessageEntity
import ir.hrka.hooshmand.model.ChatMessage
import ir.hrka.hooshmand.model.ChatMessageRole

/**
 * Maps a Room [MessageEntity] to the shared domain [ChatMessage] model.
 */
internal fun MessageEntity.asExternalModel(): ChatMessage =
    ChatMessage(
        id = id,
        conversationId = conversationId,
        role = ChatMessageRole.valueOf(role),
        text = text,
        createdAt = createdAt,
    )

/**
 * Maps a domain [ChatMessage] to a Room [MessageEntity].
 */
internal fun ChatMessage.asEntity(): MessageEntity =
    MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        text = text,
        createdAt = createdAt,
    )
