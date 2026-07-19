package ir.hrka.hooshmand.ai_chat.impl

import ir.hrka.hooshmand.model.ChatMessage
import ir.hrka.hooshmand.model.ChatMessageRole
import ir.hrka.llm.runtime.api.LlmHistoryMessage
import ir.hrka.llm.runtime.api.LlmHistoryRole

/**
 * Maps a persisted [ChatMessage] to an on-screen [AiChatMessage].
 */
internal fun ChatMessage.toUiMessage(): AiChatMessage =
    AiChatMessage(
        id = id,
        role = role.toUiRole(),
        text = text,
        isStreaming = false,
        createdAt = createdAt,
    )

/**
 * Maps an on-screen [AiChatMessage] to a persisted [ChatMessage].
 *
 * @param conversationId Parent conversation id.
 */
internal fun AiChatMessage.toDomainMessage(conversationId: String): ChatMessage =
    ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role.toDomainRole(),
        text = text,
        createdAt = createdAt,
    )

/**
 * Maps UI messages to LiteRT seed turns.
 *
 * Skips blank text and [AiChatMessageRole.Error] rows.
 */
internal fun List<AiChatMessage>.toLlmHistoryMessages(): List<LlmHistoryMessage> =
    mapNotNull { message ->
        if (message.text.isBlank()) return@mapNotNull null
        when (message.role) {
            AiChatMessageRole.User ->
                LlmHistoryMessage(role = LlmHistoryRole.USER, text = message.text)

            AiChatMessageRole.Model ->
                LlmHistoryMessage(role = LlmHistoryRole.MODEL, text = message.text)

            AiChatMessageRole.Error -> null
        }
    }

private fun ChatMessageRole.toUiRole(): AiChatMessageRole =
    when (this) {
        ChatMessageRole.USER -> AiChatMessageRole.User
        ChatMessageRole.MODEL -> AiChatMessageRole.Model
        ChatMessageRole.ERROR -> AiChatMessageRole.Error
    }

private fun AiChatMessageRole.toDomainRole(): ChatMessageRole =
    when (this) {
        AiChatMessageRole.User -> ChatMessageRole.USER
        AiChatMessageRole.Model -> ChatMessageRole.MODEL
        AiChatMessageRole.Error -> ChatMessageRole.ERROR
    }
