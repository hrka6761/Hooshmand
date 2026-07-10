package ir.hrka.hooshmand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import dagger.hilt.android.AndroidEntryPoint
import ir.hrka.hooshmand.home.api.HomeNavKey
import ir.hrka.hooshmand.navigation.Navigator
import ir.hrka.hooshmand.navigation.rememberNavigationState
import ir.hrka.hooshmand.ui.theme.HooshmandTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HooshmandTheme {
                val navigationState = rememberNavigationState(
                    HomeNavKey,
                    setOf(HomeNavKey)
                )
                val navigator = remember { Navigator(navigationState) }

                HooshmandApp(
                    navigationState = navigationState,
                    navigator = navigator
                )
            }
        }
    }
}
