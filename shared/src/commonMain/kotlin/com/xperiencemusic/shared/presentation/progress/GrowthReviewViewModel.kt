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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
)

data class LevelRequestUi(
    val key: String,
    val requestId: String,
    val memberName: String,
    val instrument: Instrument?,
    val voicePart: VoicePart?,
    val target: Level,
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
)

sealed interface GrowthReviewEvent {
    data class CommentChanged(val key: String, val comment: String) : GrowthReviewEvent
    data class Answered(val key: String, val validated: Boolean) : GrowthReviewEvent
    data object ErrorShown : GrowthReviewEvent
}

/** Only what still waits for an answer, oldest first. */
fun deriveGrowthReview(review: GrowthReview, objectives: List<Objective>): GrowthReviewUiState {
    val byId = objectives.associateBy { it.id }
    return GrowthReviewUiState(
        isLoading = false,
        requests = review.requests.filter { it.status == ExerciseStatus.SUBMITTED }.sortedBy { it.submittedOn }.map {
            LevelRequestUi("level-${it.id}", it.id, it.memberName, it.instrument, it.voicePart, it.targetLevel)
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

class GrowthReviewViewModel(
    observeReview: ObserveGrowthReviewUseCase,
    private val objectives: List<Objective>,
    private val reviewRequest: ReviewLevelRequestUseCase,
    private val reviewObjective: ReviewObjectiveUseCase,
) : ViewModel() {
    private val form = MutableStateFlow(GrowthReviewUiState(isLoading = false))

    val state: StateFlow<GrowthReviewUiState> = combine(observeReview(), form) { review, f ->
        deriveGrowthReview(review, objectives).copy(
            comments = f.comments, savingKey = f.savingKey, missingCommentKey = f.missingCommentKey, hasError = f.hasError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthReviewUiState())

    fun onEvent(event: GrowthReviewEvent) {
        when (event) {
            is GrowthReviewEvent.CommentChanged -> form.update {
                it.copy(comments = it.comments + (event.key to event.comment), missingCommentKey = null)
            }
            is GrowthReviewEvent.Answered -> answer(event.key, event.validated)
            GrowthReviewEvent.ErrorShown -> form.update { it.copy(hasError = false) }
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
            val result = current.requests.firstOrNull { it.key == key }?.let { reviewRequest(it.requestId, validated, comment) }
                ?: current.objectives.firstOrNull { it.key == key }?.let { reviewObjective(it.memberId, it.objectiveId, validated, comment) }
            form.update {
                it.copy(savingKey = null, hasError = result is DomainResult.Failure, comments = it.comments - key)
            }
        }
    }
}
