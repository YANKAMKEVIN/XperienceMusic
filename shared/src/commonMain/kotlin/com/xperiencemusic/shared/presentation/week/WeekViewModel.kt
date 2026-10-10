package com.xperiencemusic.shared.presentation.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.usecase.ObserveWeeklySetlistUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WeekViewModel(observeWeeklySetlist: ObserveWeeklySetlistUseCase) : ViewModel() {
    val state: StateFlow<WeekUiState> = observeWeeklySetlist()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUiState())
}
