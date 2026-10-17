package com.xperiencemusic.shared.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveGrowthReviewUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveMyGrowthUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import com.xperiencemusic.shared.domain.usecase.RequestLevelUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSingerPath: ObserveSingerPathUseCase,
    observeSchedule: ObserveSetlistScheduleUseCase,
    submissions: VoiceSubmissionRepository,
    observeMyGrowth: ObserveMyGrowthUseCase,
    observeGrowthReview: ObserveGrowthReviewUseCase,
    private val requestLevel: RequestLevelUseCase,
    private val tickObjective: TickObjectiveUseCase,
    learningRepository: com.xperiencemusic.shared.domain.learning.LearningRepository,
) : ViewModel() {
    private val action = MutableStateFlow(ActionState())

    private val base = observeCurrentMember()
        .flatMapLatest { member ->
            if (member == null) {
                flowOf(deriveProgressState(null, null))
            } else {
                // Only the super admin may read everyone's submissions and growth.
                val isReviewer = member.role.can(Permission.REVIEW_TRAINING)
                val all = if (isReviewer) submissions.observeAll() else flowOf(emptyList())
                val review = if (isReviewer) observeGrowthReview() else flowOf(null)
                combine(
                    combine(observeSingerPath(member.id), observeSchedule(), ::Pair),
                    submissions.observeMine(member.id),
                    all,
                    observeMyGrowth(),
                    review,
                ) { (path, schedule), mine, everyone, growth, queue ->
                    deriveProgressState(member, path)
                        .withVoices(schedule.current?.songs.orEmpty(), mine, everyone)
                        .withGrowth(growth, queue)
                }.onStart { emit(deriveProgressState(member, null)) }
            }
        }

    private val learning = observeCurrentMember().flatMapLatest { member ->
        if (member?.profile?.instrument == com.xperiencemusic.shared.domain.model.Instrument.PIANO) {
            learningRepository.observe(member.id).map { it as com.xperiencemusic.shared.domain.learning.LearningProgress? }
        } else flowOf(null)
    }

    val state: StateFlow<ProgressUiState> = combine(base, action, learning) { state, act, course ->
        state.copy(isWorking = act.isWorking, hasError = act.hasError, learning = course)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun onEvent(event: ProgressEvent) {
        when (event) {
            ProgressEvent.AskLevelUpClicked -> run { requestLevel() }
            is ProgressEvent.ObjectiveToggled -> run { tickObjective(event.objectiveId, event.reached) }
            ProgressEvent.ErrorShown -> action.update { it.copy(hasError = false) }
        }
    }

    private fun run(block: suspend () -> DomainResult<Unit>) {
        if (action.value.isWorking) return
        action.value = ActionState(isWorking = true)
        viewModelScope.launch {
            action.value = ActionState(hasError = block() is DomainResult.Failure)
        }
    }

    private data class ActionState(val isWorking: Boolean = false, val hasError: Boolean = false)
}
