package com.xperiencemusic.shared.presentation.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.usecase.ObserveHelpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HelpViewModel(observeHelp: ObserveHelpUseCase) : ViewModel() {
    private val chosen = MutableStateFlow<HelpAudience?>(null)

    val state: StateFlow<HelpUiState> = combine(observeHelp(), chosen, ::deriveHelpState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HelpUiState())

    fun onEvent(event: HelpEvent) {
        when (event) {
            is HelpEvent.AudienceSelected -> chosen.value = event.audience
        }
    }
}
