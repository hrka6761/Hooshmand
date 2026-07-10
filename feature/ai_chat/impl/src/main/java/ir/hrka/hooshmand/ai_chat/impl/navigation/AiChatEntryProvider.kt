package ir.hrka.hooshmand.ai_chat.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.ai_chat.api.AiChatNavKey
import ir.hrka.hooshmand.ai_chat.impl.AiChatScreen
import ir.hrka.hooshmand.navigation.Navigator

fun EntryProviderScope<NavKey>.aiChatEntry(navigator: Navigator) {
    entry<AiChatNavKey> {
        AiChatScreen(
            onNavigateHome = { navigator.goBack() },
        )
    }
}
