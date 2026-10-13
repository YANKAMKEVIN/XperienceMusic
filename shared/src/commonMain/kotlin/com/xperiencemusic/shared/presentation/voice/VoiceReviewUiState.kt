package com.xperiencemusic.shared.presentation.voice

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.common.PlaybackStatus

data class VoiceReviewUiState(
    val isLoading: Boolean = true,
    /** Waiting for an answer, oldest first. */
    val toReview: List<SubmissionUi> = emptyList(),
    /** Already answered, most recent first. */
    val reviewed: List<SubmissionUi> = emptyList(),
    val hasError: Boolean = false,
)

data class SubmissionUi(
    val id: String,
    val memberName: String,
    val voicePart: VoicePart?,
    val songTitle: String,
    val date: String,
    val duration: String,
    val status: ExerciseStatus,
    val feedback: String?,
    val playback: PlaybackStatus = PlaybackStatus.IDLE,
    /** The comment being written, before it is sent. */
    val draft: String = "",
    val isSaving: Boolean = false,
    /** Sending work back needs a comment. */
    val needsComment: Boolean = false,
)

sealed interface VoiceReviewEvent {
    data class PlayClicked(val id: String) : VoiceReviewEvent
    data class DraftChanged(val id: String, val text: String) : VoiceReviewEvent
    data class ValidateClicked(val id: String) : VoiceReviewEvent
    data class ReworkClicked(val id: String) : VoiceReviewEvent
    data object ErrorShown : VoiceReviewEvent
}
