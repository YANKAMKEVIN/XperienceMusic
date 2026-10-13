package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.StepState
import com.xperiencemusic.shared.domain.model.VoicePart

data class ProgressUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val instrument: Instrument? = null,
    val level: Level? = null,
    val voicePart: VoicePart? = null,
    /** False for musicians: their path is not available yet. */
    val hasPath: Boolean = false,
    /** Days of practice in a row. */
    val streakDays: Int = 0,
    val completedTiers: Int = 0,
    val warmUp: WarmUpUi? = null,
    val tiers: List<TierUi> = emptyList(),
    val canManageMembers: Boolean = false,
    /** The current week's songs, with what this singer sent on each. */
    val weekVoices: List<WeekVoiceUi> = emptyList(),
    val canReview: Boolean = false,
    /** Submissions waiting for the super admin. */
    val toReviewCount: Int = 0,
)

data class WeekVoiceUi(
    val songId: String,
    val title: String,
    /** Null when nothing was sent yet. */
    val status: ExerciseStatus?,
    val feedback: String?,
)

data class WarmUpUi(val id: String, val isDoneToday: Boolean)

data class TierUi(
    /** 1-based; the title comes from the screen's strings. */
    val number: Int,
    val isComplete: Boolean,
    val steps: List<StepUi>,
)

data class StepUi(
    val id: String,
    val title: String,
    /** "La3 · 5 s", "5 notes" */
    val detail: String,
    val state: StepState,
    val isChallenge: Boolean,
)
