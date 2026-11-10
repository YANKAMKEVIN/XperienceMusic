package com.xperiencemusic.shared.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.usecase.GetLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveLearningProgressUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveMyGrowthUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveMySubmissionsUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveReviewQueueUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import com.xperiencemusic.shared.domain.usecase.RequestLevelUseCase
import com.xperiencemusic.shared.domain.usecase.SyncLearningUseCase
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
    observeMySubmissions: ObserveMySubmissionsUseCase,
    observeReviewQueue: ObserveReviewQueueUseCase,
    observeMyGrowth: ObserveMyGrowthUseCase,
    private val requestLevel: RequestLevelUseCase,
    private val tickObjective: TickObjectiveUseCase,
    observeLearningProgress: ObserveLearningProgressUseCase,
    syncLearning: SyncLearningUseCase,
    getLearningPreferences: GetLearningPreferencesUseCase,
) : ViewModel() {
    private val action = MutableStateFlow(ActionState())

    private val base = observeCurrentMember()
        .flatMapLatest { member ->
            if (member == null) {
                flowOf(deriveProgressState(null, null))
            } else {
                // The queue stays empty for whoever may not read the others' work.
                combine(
                    combine(observeSingerPath(member.id), observeSchedule(), ::Pair),
                    observeMySubmissions(),
                    observeReviewQueue(),
                    observeMyGrowth(),
                ) { (path, schedule), mine, queue, growth ->
                    deriveProgressState(member, path)
                        .withVoices(schedule.current?.songs.orEmpty(), mine)
                        .withGrowth(growth)
                        .withReviewQueue(queue)
                }.onStart { emit(deriveProgressState(member, null)) }
            }
        }

    private val learning = observeCurrentMember().flatMapLatest { member ->
        if (member != null && LearningCatalog.modulesFor(member.profile.instrument).isNotEmpty()) {
            // Fetches the history kept on the server too: a fresh phone would otherwise show 0 done.
            observeLearningProgress(member.id)
                .onStart { viewModelScope.launch { syncLearning(member.id) } }
                .map<LearningProgress, LearningProgress?> { it }
        } else {
            flowOf(null)
        }
    }

    // The comfort settings live with the guided path; read again each time the screen comes back.
    private val reduceMotion = observeCurrentMember().map { member ->
        member != null && getLearningPreferences(member.id).reduceMotion
    }

    val state: StateFlow<ProgressUiState> = combine(base, action, learning, reduceMotion) { state, act, course, calm ->
        state.copy(isWorking = act.isWorking, hasError = act.hasError, learning = course, reduceMotion = calm)
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
