package com.xperiencemusic.shared.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}

class AppViewModel(
    observeSession: ObserveSessionUseCase,
    private val retrySession: RetrySessionUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {
    val state: StateFlow<AppUiState> = observeSession()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    fun onEvent(event: AppEvent) {
        viewModelScope.launch {
            when (event) {
                AppEvent.RetryClicked -> retrySession()
                AppEvent.SignOutClicked -> signOut()
            }
        }
    }
}

fun SessionState.toUiState(): AppUiState = when (this) {
    SessionState.Loading -> AppUiState.Loading
    SessionState.SignedOut -> AppUiState.SignIn
    SessionState.Unreachable -> AppUiState.Unreachable
    is SessionState.NotInvited -> AppUiState.NotInvited(email)
    is SessionState.NeedsProfile -> AppUiState.Onboarding
    is SessionState.Ready -> AppUiState.Ready
}
