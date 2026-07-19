package ir.hrka.hooshmand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hrka.hooshmand.MainActivityUiState.CheckingUpdate
import ir.hrka.hooshmand.MainActivityUiState.Ready
import ir.hrka.hooshmand.domain.AppUpdateStatus
import ir.hrka.hooshmand.domain.CheckAppUpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds startup state for [MainActivity], including the remote app-update check.
 *
 * While [uiState] is [CheckingUpdate], the system splash screen stays on-screen.
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainActivityUiState>(CheckingUpdate)

    /**
     * Startup UI state. Starts as [CheckingUpdate], then becomes [Ready] with an
     * [AppUpdateStatus] (or [AppUpdateStatus.NoUpdate] if the check fails).
     */
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val status = runCatching {
                checkAppUpdateUseCase(BuildConfig.VERSION_CODE)
            }.getOrDefault(AppUpdateStatus.NoUpdate)
            _uiState.value = Ready(status)
        }
    }

    /**
     * Dismisses an optional update dialog so the user can continue using the app.
     */
    fun dismissOptionalUpdate() {
        val current = _uiState.value
        if (current is Ready && current.updateStatus is AppUpdateStatus.OptionalUpdate) {
            _uiState.value = Ready(AppUpdateStatus.NoUpdate)
        }
    }
}

/**
 * Startup state used to drive the splash keep-on-screen condition and update dialogs.
 */
sealed interface MainActivityUiState {

    /** Remote update check is still running; keep the splash visible. */
    data object CheckingUpdate : MainActivityUiState

    /**
     * Update check finished.
     *
     * @property updateStatus Result used by update dialogs after the splash dismisses.
     */
    data class Ready(
        val updateStatus: AppUpdateStatus,
    ) : MainActivityUiState

    /**
     * Whether the AndroidX splash screen should remain on-screen.
     */
    fun shouldKeepSplashScreen(): Boolean = this is CheckingUpdate
}
