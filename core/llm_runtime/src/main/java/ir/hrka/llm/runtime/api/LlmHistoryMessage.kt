package ir.hrka.llm.runtime.api

/**
 * One prior conversation turn used to seed a LiteRT session.
 *
 * @property role Speaker role for this turn.
 * @property text Message text content.
 */
data class LlmHistoryMessage(
    val role: LlmHistoryRole,
    val text: String,
)

/**
 * Roles that can be restored into an on-device conversation session.
 */
enum class LlmHistoryRole {
    /** Prior user turn. */
    USER,

    /** Prior model turn. */
    MODEL,
}
