package com.xperiencemusic.shared.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSingerPath: ObserveSingerPathUseCase,
) : ViewModel() {
    val state: StateFlow<ProgressUiState> = observeCurrentMember()
        .flatMapLatest { member ->
            if (member == null) {
                flowOf(deriveProgressState(null, null))
            } else {
                observeSingerPath(member.id).map { deriveProgressState(member, it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}
