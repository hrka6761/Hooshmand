package ir.hrka.hooshmand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import ir.hrka.hooshmand.home.api.HomeNavKey
import ir.hrka.hooshmand.navigation.Navigator
import ir.hrka.hooshmand.navigation.rememberNavigationState
import ir.hrka.hooshmand.ui.theme.HooshmandTheme

/**
 * Single-activity entry point.
 *
 * Installs the AndroidX splash screen and keeps it on-screen until
 * [MainActivityViewModel] finishes the remote update check.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.shouldKeepSplashScreen()
        }

        enableEdgeToEdge()
        setContent {
            HooshmandTheme {
                val navigationState = rememberNavigationState(
                    HomeNavKey,
                    setOf(HomeNavKey),
                )
                val navigator = remember { Navigator(navigationState) }

                HooshmandApp(
                    navigationState = navigationState,
                    navigator = navigator,
                )
            }
        }
    }
}
