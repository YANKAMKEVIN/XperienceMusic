package com.xperiencemusic.shared.domain.model

/** One step of a singer's path ("Ma progression"). */
data class Exercise(
    val id: String,
    val level: Level,
    val order: Int,
    val title: String,
    val instructions: String,
    val check: ExerciseCheck,
)

/** How an exercise is validated. */
sealed interface ExerciseCheck {
    /**
     * Validated automatically by pitch detection. The target is relative to the singer's
     * [VoicePart.comfortMidi], so one exercise fits every voice.
     */
    data class HoldNote(val semitonesAboveComfort: Int, val seconds: Double) : ExerciseCheck {
        fun targetMidi(voicePart: VoicePart): Int = voicePart.comfortMidi + semitonesAboveComfort
    }

    /** The singer sends a recording that a [Role.SUPER_ADMIN] reviews. */
    data object Review : ExerciseCheck
}

enum class ExerciseStatus { TODO, SUBMITTED, VALIDATED, TO_REWORK }

data class ExerciseProgress(val exercise: Exercise, val status: ExerciseStatus)
