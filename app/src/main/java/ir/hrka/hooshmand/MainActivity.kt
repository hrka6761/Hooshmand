package ir.hrka.hooshmand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
 * Installs the AndroidX splash screen and keeps it on-screen via
 * [androidx.core.splashscreen.SplashScreen.setKeepOnScreenCondition] until the app is ready
 * (update-check wiring will drive this condition later).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Stub: dismiss immediately. Update-check will keep the splash until the check finishes.
        splashScreen.setKeepOnScreenCondition { false }

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
