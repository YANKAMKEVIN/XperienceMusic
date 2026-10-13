package com.xperiencemusic.shared.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.ReviewVoiceUseCase
import com.xperiencemusic.shared.presentation.common.Formatting
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ReviewLocal(
    val drafts: Map<String, String> = emptyMap(),
    val saving: Set<String> = emptySet(),
    val needsComment: Set<String> = emptySet(),
    val hasError: Boolean = false,
)

/** The super admin's "À écouter": every singer's part, played here and answered in one place. */
class VoiceReviewViewModel(
    private val submissions: VoiceSubmissionRepository,
    private val review: ReviewVoiceUseCase,
    private val playback: PlaybackController,
) : ViewModel() {
    private val local = MutableStateFlow(ReviewLocal())
    private val all = submissions.observeAll()

    val state: StateFlow<VoiceReviewUiState> = combine(all, local, playback.state, ::deriveVoiceReviewState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VoiceReviewUiState())

    fun onEvent(event: VoiceReviewEvent) {
        when (event) {
            is VoiceReviewEvent.PlayClicked -> viewModelScope.launch {
                all.first().firstOrNull { it.id == event.id }?.let {
                    playback.playOrToggle(PlaybackTrack("voice-${it.id}", it.memberName, it.songTitle, it.audioPath))
                }
            }
            is VoiceReviewEvent.DraftChanged -> local.update {
                it.copy(drafts = it.drafts + (event.id to event.text), needsComment = it.needsComment - event.id)
            }
            is VoiceReviewEvent.ValidateClicked -> answer(event.id, validated = true)
            is VoiceReviewEvent.ReworkClicked -> answer(event.id, validated = false)
            VoiceReviewEvent.ErrorShown -> local.update { it.copy(hasError = false) }
        }
    }

    private fun answer(id: String, validated: Boolean) {
        if (id in local.value.saving) return
        val draft = local.value.drafts[id].orEmpty()
        local.update { it.copy(saving = it.saving + id) }
        viewModelScope.launch {
            val result = review(id, validated, draft)
            local.update { current ->
                when (result) {
                    is DomainResult.Success -> current.copy(saving = current.saving - id, drafts = current.drafts - id)
                    is DomainResult.Failure -> current.copy(
                        saving = current.saving - id,
                        needsComment = if (result.error == DomainError.INVALID_INPUT) current.needsComment + id else current.needsComment,
                        hasError = result.error != DomainError.INVALID_INPUT,
                    )
                }
            }
        }
    }
}

internal fun deriveVoiceReviewState(
    all: List<VoiceSubmission>,
    local: ReviewLocal,
    playing: PlaybackState,
): VoiceReviewUiState {
    fun VoiceSubmission.toUi() = SubmissionUi(
        id = id,
        memberName = memberName,
        voicePart = voicePart,
        songTitle = songTitle,
        date = Formatting.dayAndDate(submittedOn),
        duration = Formatting.duration(durationSeconds),
        status = status,
        feedback = feedback,
        playback = when {
            playing.track?.id != "voice-$id" -> PlaybackStatus.IDLE
            playing.isLoading -> PlaybackStatus.LOADING
            playing.isPlaying -> PlaybackStatus.PLAYING
            else -> PlaybackStatus.PAUSED
        },
        draft = local.drafts[id].orEmpty(),
        isSaving = id in local.saving,
        needsComment = id in local.needsComment,
    )
    val (waiting, done) = all.partition { it.status == ExerciseStatus.SUBMITTED }
    return VoiceReviewUiState(
        isLoading = false,
        toReview = waiting.sortedBy { it.submittedOn }.map { it.toUi() },
        reviewed = done.sortedByDescending { it.submittedOn }.map { it.toUi() },
        hasError = local.hasError,
    )
}
