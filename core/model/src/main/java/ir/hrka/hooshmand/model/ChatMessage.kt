package ir.hrka.hooshmand.model

/**
 * A persisted chat message belonging to a [Conversation].
 *
 * Streaming UI state is not part of this model; it lives only in the feature layer.
 *
 * @property id Stable unique id (UUID string).
 * @property conversationId Parent [Conversation.id].
 * @property role Sender or message kind.
 * @property text Message body.
 * @property createdAt Epoch millis when the message was created.
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: ChatMessageRole,
    val text: String,
    val createdAt: Long,
)
