package com.xperiencemusic.shared.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Objective
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.usecase.GrowthReview
import com.xperiencemusic.shared.domain.usecase.ObserveGrowthReviewUseCase
import com.xperiencemusic.shared.domain.usecase.ReviewLevelRequestUseCase
import com.xperiencemusic.shared.domain.usecase.ReviewObjectiveUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveRecordingsUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.presentation.learning.ObjectiveTakeUi
import com.xperiencemusic.shared.presentation.learning.objectiveTakes
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

data class GrowthReviewUiState(
    val isLoading: Boolean = true,
    val requests: List<LevelRequestUi> = emptyList(),
    val objectives: List<ObjectiveReviewUi> = emptyList(),
    /** Comments being typed, by item key. */
    val comments: Map<String, String> = emptyMap(),
    val savingKey: String? = null,
    /** The item whose send-back lacked a comment. */
    val missingCommentKey: String? = null,
    val hasError: Boolean = false,
    /** The answer just saved, said once so the vanished card is not a mystery. */
    val lastAnswer: GrowthAnswerUi? = null,
)

data class GrowthAnswerUi(val memberName: String, val validated: Boolean, val isLevel: Boolean)

/** One objective of the level asked for; [reached] is null when the microphone tiers decide it. */
data class RequestObjectiveUi(val title: String, val reached: Boolean?)

data class LevelRequestUi(
    val key: String,
    val requestId: String,
    val memberName: String,
    val instrument: Instrument?,
    val voicePart: VoicePart?,
    val target: Level,
    /** What the member did to get there, checked before confirming. */
    val objectives: List<RequestObjectiveUi> = emptyList(),
)

data class ObjectiveReviewUi(
    val key: String,
    val memberId: String,
    val memberName: String,
    val instrument: Instrument?,
    val objectiveId: String,
    val title: String,
    /** What to check before validating. */
    val criterion: String,
    val target: Level?,
    /** The takes the member sent for it, newest first. */
    val takes: List<ObjectiveTakeUi> = emptyList(),
)

sealed interface GrowthReviewEvent {
    data class CommentChanged(val key: String, val comment: String) : GrowthReviewEvent
    data class Answered(val key: String, val validated: Boolean) : GrowthReviewEvent
    data object ErrorShown : GrowthReviewEvent
    data object AnswerShown : GrowthReviewEvent

    /** Plays a take sent for an objective, or pauses it; the screen names it for the system controls. */
    data class PlayTake(val take: ObjectiveTakeUi, val title: String, val subtitle: String) : GrowthReviewEvent
}

/** Only what still waits for an answer, oldest first. */
fun deriveGrowthReview(review: GrowthReview, objectives: List<Objective>): GrowthReviewUiState {
    val byId = objectives.associateBy { it.id }
    return GrowthReviewUiState(
        isLoading = false,
        requests = review.requests.filter { it.status == ExerciseStatus.SUBMITTED }.sortedBy { it.submittedOn }.map { request ->
            val needed = objectives.filter { it.instrument == request.instrument && it.level == request.targetLevel }.sortedBy { it.order }
            LevelRequestUi(
                "level-${request.id}", request.id, request.memberName, request.instrument, request.voicePart, request.targetLevel,
                needed.map { objective ->
                    RequestObjectiveUi(
                        objective.title,
                        if (objective.autoTier != null) {
                            null
                        } else {
                            review.checks.any {
                                it.memberId == request.memberId && it.objectiveId == objective.id && it.status == ExerciseStatus.VALIDATED
                            }
                        },
                    )
                },
            )
        },
        objectives = review.checks.filter { it.status == ExerciseStatus.SUBMITTED }.sortedBy { it.submittedOn }.map { check ->
            val objective = byId[check.objectiveId]
            ObjectiveReviewUi(
                "objective-${check.memberId}-${check.objectiveId}", check.memberId, check.memberName, check.instrument,
                check.objectiveId, objective?.title ?: check.objectiveId, objective?.criterion.orEmpty(), objective?.level,
            )
        },
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class GrowthReviewViewModel(
    observeReview: ObserveGrowthReviewUseCase,
    private val objectives: List<Objective>,
    private val reviewRequest: ReviewLevelRequestUseCase,
    private val reviewObjective: ReviewObjectiveUseCase,
    private val observeRecordings: ObserveObjectiveRecordingsUseCase,
    private val playback: PlaybackController,
) : ViewModel() {
    private val form = MutableStateFlow(GrowthReviewUiState(isLoading = false))

    private val review = observeReview().map { deriveGrowthReview(it, objectives) }

    /** The takes of every member waiting for an objective review, by member. */
    private val recordings = review.flatMapLatest { derived ->
        val members = derived.objectives.map { it.memberId }.distinct()
        if (members.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(members.map { member -> observeRecordings(member).map { member to it } }) { it.toMap() }
        }
    }.onStart { emit(emptyMap()) }

    val state: StateFlow<GrowthReviewUiState> = combine(review, recordings, playback.state, form) { derived, takes, playing, f ->
        derived.copy(
            objectives = derived.objectives.map { objective ->
                objective.copy(takes = objectiveTakes(takes[objective.memberId].orEmpty(), objective.objectiveId, playing))
            },
            comments = f.comments, savingKey = f.savingKey, missingCommentKey = f.missingCommentKey, hasError = f.hasError,
            lastAnswer = f.lastAnswer,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthReviewUiState())

    fun onEvent(event: GrowthReviewEvent) {
        when (event) {
            is GrowthReviewEvent.CommentChanged -> form.update {
                it.copy(comments = it.comments + (event.key to event.comment), missingCommentKey = null)
            }
            is GrowthReviewEvent.Answered -> answer(event.key, event.validated)
            GrowthReviewEvent.ErrorShown -> form.update { it.copy(hasError = false) }
            GrowthReviewEvent.AnswerShown -> form.update { it.copy(lastAnswer = null) }
            is GrowthReviewEvent.PlayTake -> playback.playOrToggle(event.take.track(event.title, event.subtitle))
        }
    }

    private fun answer(key: String, validated: Boolean) {
        val current = state.value
        if (current.savingKey != null) return
        val comment = current.comments[key].orEmpty()
        if (!validated && comment.isBlank()) {
            form.update { it.copy(missingCommentKey = key) }
            return
        }
        form.update { it.copy(savingKey = key, missingCommentKey = null) }
        viewModelScope.launch {
            val request = current.requests.firstOrNull { it.key == key }
            val objective = current.objectives.firstOrNull { it.key == key }
            val result = request?.let { reviewRequest(it.requestId, validated, comment) }
                ?: objective?.let { reviewObjective(it.memberId, it.objectiveId, validated, comment) }
            val answer = (request?.memberName ?: objective?.memberName)?.let { GrowthAnswerUi(it, validated, isLevel = request != null) }
            form.update {
                it.copy(
                    savingKey = null,
                    hasError = result is DomainResult.Failure,
                    comments = it.comments - key,
                    lastAnswer = if (result is DomainResult.Success) answer else null,
                )
            }
        }
    }
}
