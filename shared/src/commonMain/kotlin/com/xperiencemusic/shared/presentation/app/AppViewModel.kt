package com.xperiencemusic.shared.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.usecase.ObserveRemoteLoadFailureUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RetryRemoteLoadsUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import com.xperiencemusic.shared.domain.usecase.SyncServiceRemindersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.xperiencemusic.shared.domain.usecase.CatchUpUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

sealed interface AppUiState {
    data object Loading : AppUiState
    data object SignIn : AppUiState
    data object Unreachable : AppUiState
    data class NotInvited(val email: String) : AppUiState
    data object Onboarding : AppUiState
    data object Ready : AppUiState
}

sealed interface AppEvent {
    data object RetryClicked : AppEvent
    data object SignOutClicked : AppEvent
    data object SignOutConfirmed : AppEvent
    data object SignOutDismissed : AppEvent

    /** "Réessayer" on the banner shown when some data could not be fetched. */
    data object RetryLoadsClicked : AppEvent

    /** The app came (back) to the screen: what failed while it was away is fetched again. */
    data object AppForegrounded : AppEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    observeSession: ObserveSessionUseCase,
    private val retrySession: RetrySessionUseCase,
    private val signOut: SignOutUseCase,
    observeLoadFailure: ObserveRemoteLoadFailureUseCase,
    private val retryLoads: RetryRemoteLoadsUseCase,
    syncReminders: SyncServiceRemindersUseCase,
    private val catchUp: CatchUpUseCase,
) : ViewModel() {
    init {
        // After a failure, try again a few times on our own: the network usually comes back within
        // seconds (a phone waking up, a lift, a tunnel). The banner still offers "Réessayer".
        viewModelScope.launch {
            observeSession()
                .map { it is SessionState.Ready }
                .distinctUntilChanged()
                .flatMapLatest { isSignedIn -> if (isSignedIn) catchUp.failures() else flowOf(false) }
                .collectLatest { isFailing ->
                    if (isFailing) {
                        for (wait in RETRY_DELAYS_MILLIS) {
                            delay(wait)
                            catchUp()
                        }
                    }
                }
        }

        // The phone's planning reminders follow the planning for as long as someone is signed in.
        observeSession()
            .map { it is SessionState.Ready }
            .distinctUntilChanged()
            .flatMapLatest { isSignedIn -> if (isSignedIn) syncReminders() else emptyFlow() }
            .launchIn(viewModelScope)
    }

    val state: StateFlow<AppUiState> = observeSession()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    private val _isConfirmingSignOut = MutableStateFlow(false)

    /** "Se déconnecter ?" is on screen: signing out wipes what the phone has not synced yet. */
    val isConfirmingSignOut: StateFlow<Boolean> = _isConfirmingSignOut.asStateFlow()

    /** Some data could not be fetched: the signed-in shell says screens may be incomplete. */
    val hasLoadFailure: StateFlow<Boolean> = observeLoadFailure()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onEvent(event: AppEvent) {
        when (event) {
            AppEvent.RetryClicked -> viewModelScope.launch { retrySession() }
            AppEvent.SignOutClicked ->
                if (state.value.confirmsSignOut) _isConfirmingSignOut.value = true else signOutNow()
            AppEvent.SignOutConfirmed -> signOutNow()
            AppEvent.SignOutDismissed -> _isConfirmingSignOut.value = false
            AppEvent.RetryLoadsClicked -> viewModelScope.launch { retryLoads() }
            AppEvent.AppForegrounded -> if (state.value == AppUiState.Ready) viewModelScope.launch { catchUp() }
        }
    }

    private fun signOutNow() {
        _isConfirmingSignOut.value = false
        viewModelScope.launch { signOut() }
    }
}

/** Spaced out: a dead network is not hammered, a short gap is caught within seconds. */
private val RETRY_DELAYS_MILLIS = listOf(3_000L, 10_000L, 30_000L)

/**
 * Only a member who got into the app can have something to lose (unsynced attempts, downloads):
 * leaving the onboarding or the "not invited" screen needs no confirmation.
 */
val AppUiState.confirmsSignOut: Boolean
    get() = this == AppUiState.Ready || this == AppUiState.Unreachable

/**
 * The root screen a state shows. Each one owns its ViewModels, dropped when the app moves to
 * another: a new session never inherits the previous sign-in, onboarding or signed-in screens.
 */
val AppUiState.destination: String
    get() = when (this) {
        AppUiState.Loading -> "loading"
        AppUiState.SignIn -> "sign-in"
        AppUiState.Unreachable -> "unreachable"
        is AppUiState.NotInvited -> "not-invited"
        AppUiState.Onboarding -> "onboarding"
        AppUiState.Ready -> "ready"
    }

fun SessionState.toUiState(): AppUiState = when (this) {
    SessionState.Loading -> AppUiState.Loading
    SessionState.SignedOut -> AppUiState.SignIn
    SessionState.Unreachable -> AppUiState.Unreachable
    is SessionState.NotInvited -> AppUiState.NotInvited(email)
    is SessionState.NeedsProfile -> AppUiState.Onboarding
    is SessionState.Ready -> AppUiState.Ready
}
