package ir.hrka.hooshmand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hrka.hooshmand.MainActivityUiState.CheckingUpdate
import ir.hrka.hooshmand.MainActivityUiState.Ready
import ir.hrka.hooshmand.domain.AppUpdateStatus
import ir.hrka.hooshmand.domain.CheckAppUpdateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
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

    /**
     * Startup UI state. Starts as [CheckingUpdate], then becomes [Ready] with an
     * [AppUpdateStatus] (or [AppUpdateStatus.NoUpdate] if the check fails).
     */
    val uiState: StateFlow<MainActivityUiState> = flow {
        val status = runCatching {
            checkAppUpdateUseCase(BuildConfig.VERSION_CODE)
        }.getOrDefault(AppUpdateStatus.NoUpdate)
        emit(Ready(status))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CheckingUpdate,
    )
}

/**
 * Startup state used to drive the splash keep-on-screen condition and later update dialogs.
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
