package ir.hrka.hooshmand.ai_chat_history.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.ai_chat.api.AiChatNavKey
import ir.hrka.hooshmand.ai_chat_history.api.AiChatHistoryNavKey
import ir.hrka.hooshmand.ai_chat_history.impl.AiChatHistoryScreen
import ir.hrka.hooshmand.navigation.Navigator
import java.util.UUID

/**
 * Registers the AI chat history destination.
 *
 * @param navigator App navigator for opening chats and going back.
 */
fun EntryProviderScope<NavKey>.aiChatHistoryEntry(navigator: Navigator) {
    entry<AiChatHistoryNavKey> {
        AiChatHistoryScreen(
            onConversationClick = { conversationId ->
                navigator.navigate(AiChatNavKey(conversationId = conversationId))
            },
            onNewChat = {
                navigator.navigate(AiChatNavKey(conversationId = UUID.randomUUID().toString()))
            },
            onBack = { navigator.goBack() },
        )
    }
}
