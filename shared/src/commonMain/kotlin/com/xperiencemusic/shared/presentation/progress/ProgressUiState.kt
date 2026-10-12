package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart

data class ProgressUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val instrument: Instrument? = null,
    val level: Level? = null,
    val voicePart: VoicePart? = null,
    /** False for musicians: their path is not available yet. */
    val hasPath: Boolean = false,
    val exercises: List<ExerciseUi> = emptyList(),
    val validatedCount: Int = 0,
    val canManageMembers: Boolean = false,
    /** What "Start the exercise" opens: work sent back first, then the first one to do. */
    val nextExerciseId: String? = null,
)

data class ExerciseUi(
    val id: String,
    val position: Int,
    val title: String,
    val instructions: String,
    val check: ExerciseCheckUi,
    val status: ExerciseStatus,
)

sealed interface ExerciseCheckUi {
    data class HoldNote(val noteLabel: String, val seconds: Int) : ExerciseCheckUi
    data object Review : ExerciseCheckUi
}
