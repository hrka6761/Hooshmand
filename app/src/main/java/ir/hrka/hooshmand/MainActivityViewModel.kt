package ir.hrka.hooshmand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hrka.hooshmand.MainActivityUiState.CheckingUpdate
import ir.hrka.hooshmand.MainActivityUiState.ContentReady
import ir.hrka.hooshmand.MainActivityUiState.UpdateRequired
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
 * The system splash stays on-screen for [CheckingUpdate] and [UpdateRequired] (with the update
 * dialog above it). Home content is only shown in [ContentReady].
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainActivityUiState>(CheckingUpdate)

    /**
     * Startup UI state. Starts as [CheckingUpdate], then becomes [UpdateRequired] or
     * [ContentReady] (or [ContentReady] with no update if the check fails).
     */
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val status = runCatching {
                checkAppUpdateUseCase(BuildConfig.VERSION_CODE)
            }.getOrDefault(AppUpdateStatus.NoUpdate)

            _uiState.value =
                when (status) {
                    AppUpdateStatus.NoUpdate -> ContentReady
                    is AppUpdateStatus.OptionalUpdate,
                    is AppUpdateStatus.MandatoryUpdate,
                    -> UpdateRequired(status)
                }
        }
    }

    /**
     * Dismisses an optional update dialog and continues into the app (home).
     */
    fun dismissOptionalUpdate() {
        val current = _uiState.value
        if (current is UpdateRequired &&
            current.updateStatus is AppUpdateStatus.OptionalUpdate
        ) {
            _uiState.value = ContentReady
        }
    }
}

/**
 * Startup state used to drive the splash keep-on-screen condition, update dialogs, and
 * whether home content may be shown.
 */
sealed interface MainActivityUiState {

    /** Remote update check is still running; keep the splash visible. */
    data object CheckingUpdate : MainActivityUiState

    /**
     * Update check finished and an update dialog must be shown before home.
     *
     * @property updateStatus Optional or mandatory update result.
     */
    data class UpdateRequired(
        val updateStatus: AppUpdateStatus,
    ) : MainActivityUiState

    /**
     * User may use the app; show home navigation content.
     *
     * Reached when there is no update, or after dismissing an optional update.
     */
    data object ContentReady : MainActivityUiState
}
