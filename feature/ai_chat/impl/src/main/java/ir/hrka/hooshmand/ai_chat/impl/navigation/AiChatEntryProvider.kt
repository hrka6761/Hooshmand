package ir.hrka.hooshmand.ai_chat.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.ai_chat.api.AiChatNavKey
import ir.hrka.hooshmand.ai_chat.impl.AiChatScreen
import ir.hrka.hooshmand.navigation.Navigator

/**
 * Registers the AI chat destination.
 *
 * @param navigator App navigator for back navigation.
 */
fun EntryProviderScope<NavKey>.aiChatEntry(navigator: Navigator) {
    entry<AiChatNavKey> { key ->
        AiChatScreen(
            conversationId = key.conversationId,
            onNavigateBack = { navigator.goBack() },
        )
    }
}
