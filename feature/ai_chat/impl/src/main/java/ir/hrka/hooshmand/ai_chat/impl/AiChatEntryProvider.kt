package ir.hrka.hooshmand.ai_chat.impl

import androidx.compose.material3.Text
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.ai_chat.api.AiChatNavKey
import ir.hrka.hooshmand.navigation.Navigator

fun EntryProviderScope<NavKey>.aiChatEntry(navigator: Navigator) {
    entry<AiChatNavKey> {
        Text(text = "AI Chat Screen")
    }
}
