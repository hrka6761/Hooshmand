package ir.hrka.hooshmand.ai_chat_history.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.ai_chat_history.api.AiChatHistoryNavKey
import ir.hrka.hooshmand.ai_chat_history.impl.AiChatHistoryScreen
import ir.hrka.hooshmand.navigation.Navigator

fun EntryProviderScope<NavKey>.aiChatHistoryEntry(navigator: Navigator) {
    entry<AiChatHistoryNavKey> {
        AiChatHistoryScreen()
    }
}
