package com.xperiencemusic.shared.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class AppUiState { LOADING, NEEDS_ONBOARDING, READY }

class AppViewModel(observeCurrentMember: ObserveCurrentMemberUseCase) : ViewModel() {
    val state: StateFlow<AppUiState> = observeCurrentMember()
        .map { member -> if (member == null) AppUiState.NEEDS_ONBOARDING else AppUiState.READY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.LOADING)
}
