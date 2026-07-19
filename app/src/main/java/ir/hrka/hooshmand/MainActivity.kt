package ir.hrka.hooshmand

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import ir.hrka.hooshmand.home.api.HomeNavKey
import ir.hrka.hooshmand.navigation.Navigator
import ir.hrka.hooshmand.navigation.rememberNavigationState
import ir.hrka.hooshmand.ui.AppUpdateDialog
import ir.hrka.hooshmand.ui.isMandatoryUpdate
import ir.hrka.hooshmand.ui.shouldShowUpdateDialog
import ir.hrka.hooshmand.ui.theme.HooshmandTheme

/**
 * Single-activity entry point.
 *
 * Installs the AndroidX splash screen and keeps it on-screen until
 * [MainActivityViewModel] finishes the remote update check, then shows an update
 * dialog when needed.
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
                val uiState by viewModel.uiState.collectAsState()

                HooshmandApp(
                    navigationState = navigationState,
                    navigator = navigator,
                )

                val readyState = uiState as? MainActivityUiState.Ready
                val updateStatus = readyState?.updateStatus
                if (updateStatus != null && updateStatus.shouldShowUpdateDialog()) {
                    val isMandatory = updateStatus.isMandatoryUpdate()
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
