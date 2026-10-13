package com.xperiencemusic.shared.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSingerPath: ObserveSingerPathUseCase,
    observeSchedule: ObserveSetlistScheduleUseCase,
    submissions: VoiceSubmissionRepository,
) : ViewModel() {
    val state: StateFlow<ProgressUiState> = observeCurrentMember()
        .flatMapLatest { member ->
            if (member == null) {
                flowOf(deriveProgressState(null, null))
            } else {
                // Only the super admin may read everyone's submissions.
                val all = if (member.role.can(Permission.REVIEW_TRAINING)) submissions.observeAll() else flowOf(emptyList())
                combine(
                    observeSingerPath(member.id),
                    observeSchedule(),
                    submissions.observeMine(member.id),
                    all,
                ) { path, schedule, mine, everyone ->
                    deriveProgressState(member, path).withVoices(schedule.current?.songs.orEmpty(), mine, everyone)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}
