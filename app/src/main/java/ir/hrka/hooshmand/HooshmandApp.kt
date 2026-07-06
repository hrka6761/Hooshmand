package ir.hrka.hooshmand

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import ir.hrka.hooshmand.home.impl.navigation.homeEntry
import ir.hrka.hooshmand.ai_chat.impl.aiChatEntry
import ir.hrka.hooshmand.navigation.NavigationState
import ir.hrka.hooshmand.navigation.Navigator
import ir.hrka.hooshmand.navigation.toEntries

@Composable
fun HooshmandApp(
    modifier: Modifier = Modifier,
    navigationState: NavigationState,
    navigator: Navigator
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val entryProvider = entryProvider {
                homeEntry(navigator)
                aiChatEntry(navigator)
            }

            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = { navigator.goBack() }
            )
        }
    }
}
