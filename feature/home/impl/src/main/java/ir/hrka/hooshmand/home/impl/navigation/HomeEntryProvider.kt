package ir.hrka.hooshmand.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.ai_chat.api.AiChatNavKey
import ir.hrka.hooshmand.home.api.HomeNavKey
import ir.hrka.hooshmand.home.impl.HomeScreen
import ir.hrka.hooshmand.navigation.Navigator

fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> {
        HomeScreen(
            navigateToAiChat = { navigator.navigate(AiChatNavKey) }
        )
    }
}
