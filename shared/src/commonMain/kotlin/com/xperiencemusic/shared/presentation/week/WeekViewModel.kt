package com.xperiencemusic.shared.presentation.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveWeeklySetlistUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class WeekViewModel(
    observeWeeklySetlist: ObserveWeeklySetlistUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
) : ViewModel() {
    val state: StateFlow<WeekUiState> = combine(observeWeeklySetlist(), observeCurrentMember()) { setlist, member ->
        setlist.toUiState().copy(canPublish = member?.role?.can(Permission.PUBLISH_SETLIST) == true)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUiState())
}
