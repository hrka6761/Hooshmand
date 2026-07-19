package ir.hrka.hooshmand.model

/**
 * A persisted AI chat conversation (one history list entry).
 *
 * @property id Stable unique id (UUID string).
 * @property title Display title; often derived from the first user message.
 * @property createdAt Epoch millis when the conversation was created.
 * @property updatedAt Epoch millis of the last message change; used for history sorting.
 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)
