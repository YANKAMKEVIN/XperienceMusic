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
    /** The way to the next level; null while loading. */
    val levelCard: LevelCardUi? = null,
    /** The objectives of the next level. */
    val objectives: List<ObjectiveUi> = emptyList(),
    /** Level-up requests and ticked objectives waiting for the super admin. */
    val toValidateCount: Int = 0,
    val isWorking: Boolean = false,
    val hasError: Boolean = false,
)

data class LevelCardUi(
    val current: Level,
    /** Null at the top level. */
    val next: Level?,
    val done: Int,
    val required: Int,
    /** Null until the member asks. */
    val requestStatus: ExerciseStatus?,
    val feedback: String?,
    val canAsk: Boolean,
)

data class ObjectiveUi(
    val id: String,
    val title: String,
    /** "Ce qu'on apprend": the help sheet, shown on demand. */
    val learn: String,
    /** What the super admin checks. */
    val criterion: String,
    /** Null while not ticked. */
    val status: ExerciseStatus?,
    val feedback: String?,
    /** Validated by a microphone tier: nothing to tick. */
    val autoTier: Int? = null,
)

sealed interface ProgressEvent {
    data object AskLevelUpClicked : ProgressEvent
    data class ObjectiveToggled(val objectiveId: String, val reached: Boolean) : ProgressEvent
    data object ErrorShown : ProgressEvent
}

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
