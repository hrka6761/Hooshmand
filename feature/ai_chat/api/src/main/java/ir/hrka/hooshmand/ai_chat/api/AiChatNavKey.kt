package ir.hrka.hooshmand.ai_chat.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation key for the AI chat screen.
 *
 * @property conversationId Stable id of the conversation to open or create.
 */
@Serializable
data class AiChatNavKey(
    val conversationId: String,
) : NavKey
