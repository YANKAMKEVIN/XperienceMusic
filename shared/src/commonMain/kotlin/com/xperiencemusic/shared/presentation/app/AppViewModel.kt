package com.xperiencemusic.shared.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
}

class AppViewModel(
    observeSession: ObserveSessionUseCase,
    private val retrySession: RetrySessionUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {
    val state: StateFlow<AppUiState> = observeSession()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    private val _isConfirmingSignOut = MutableStateFlow(false)

    /** "Se déconnecter ?" is on screen: signing out wipes what the phone has not synced yet. */
    val isConfirmingSignOut: StateFlow<Boolean> = _isConfirmingSignOut.asStateFlow()

    fun onEvent(event: AppEvent) {
        when (event) {
            AppEvent.RetryClicked -> viewModelScope.launch { retrySession() }
            AppEvent.SignOutClicked ->
                if (state.value.confirmsSignOut) _isConfirmingSignOut.value = true else signOutNow()
            AppEvent.SignOutConfirmed -> signOutNow()
            AppEvent.SignOutDismissed -> _isConfirmingSignOut.value = false
        }
    }

    private fun signOutNow() {
        _isConfirmingSignOut.value = false
        viewModelScope.launch { signOut() }
    }
}

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
