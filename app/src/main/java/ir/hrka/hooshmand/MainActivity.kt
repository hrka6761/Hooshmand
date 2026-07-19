package ir.hrka.hooshmand

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import ir.hrka.hooshmand.home.api.HomeNavKey
import ir.hrka.hooshmand.navigation.Navigator
import ir.hrka.hooshmand.navigation.rememberNavigationState
import ir.hrka.hooshmand.ui.AppUpdateDialog
import ir.hrka.hooshmand.ui.isMandatoryUpdate
import ir.hrka.hooshmand.ui.theme.HooshmandTheme
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single-activity entry point.
 *
 * Keeps the AndroidX splash on-screen during the update check and while an update dialog is
 * shown (the dialog window appears above the splash). Home is composed only in
 * [MainActivityUiState.ContentReady].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    /**
     * Splash keep flag for [SplashScreen.setKeepOnScreenCondition].
     *
     * `true` for [MainActivityUiState.CheckingUpdate] and
     * [MainActivityUiState.UpdateRequired]; `false` only for
     * [MainActivityUiState.ContentReady].
     */
    private val keepSplashOnScreen = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            keepSplashOnScreen.get()
        }

        enableEdgeToEdge()
        setContent {
            HooshmandTheme {
                val uiState by viewModel.uiState.collectAsState()

                SideEffect {
                    // Keep splash under the update dialog; dismiss only when entering the app.
                    keepSplashOnScreen.set(uiState !is MainActivityUiState.ContentReady)
                }

                when (val state = uiState) {
                    MainActivityUiState.CheckingUpdate -> Unit

                    is MainActivityUiState.UpdateRequired -> {
                        val isMandatory = state.updateStatus.isMandatoryUpdate()
                        AppUpdateDialog(
                            isMandatory = isMandatory,
                            onUpdateClick = ::openCafeBazaar,
                            onCancelClick = {
                                if (isMandatory) {
                                    finish()
                                } else {
                                    viewModel.dismissOptionalUpdate()
                                }
                            },
                        )
                    }

                    MainActivityUiState.ContentReady -> {
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
    }

    /**
     * Opens the Hooshmand listing on Cafe Bazaar.
     */
    private fun openCafeBazaar() {
        val intent = Intent(Intent.ACTION_VIEW, CAFE_BAZAAR_URL.toUri())
        startActivity(intent)
    }

    private companion object {
        const val CAFE_BAZAAR_URL = "https://cafebazaar.ir/app/ir.hrka.hooshmand"
    }
}
