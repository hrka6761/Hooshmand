package ir.hrka.hooshmand.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.hooshmand.ai_chat_history.api.AiChatHistoryNavKey
import ir.hrka.hooshmand.home.api.HomeNavKey
import ir.hrka.hooshmand.home.impl.HomeScreen
import ir.hrka.hooshmand.navigation.Navigator

/**
 * Registers the home destination.
 *
 * @param navigator App navigator used to open AI chat history.
 */
fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> {
        HomeScreen(
            navigateToAiChatHistory = { navigator.navigate(AiChatHistoryNavKey) },
        )
    }
}
