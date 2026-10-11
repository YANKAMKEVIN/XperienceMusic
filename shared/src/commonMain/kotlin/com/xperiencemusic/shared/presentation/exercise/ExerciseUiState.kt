package com.xperiencemusic.shared.presentation.exercise

import com.xperiencemusic.shared.domain.model.ExerciseStatus

data class ExerciseUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val instructions: String = "",
    val status: ExerciseStatus = ExerciseStatus.TODO,
    /** Null for exercises reviewed by the vocal coach rather than checked by the app. */
    val check: AutomaticCheckUi? = null,
    val phase: ExercisePhase = ExercisePhase.READY,
    val error: ExerciseError? = null,
)

data class AutomaticCheckUi(
    val targetNoteLabel: String,
    val targetMidi: Int,
    val requiredSeconds: Int,
    /** What the microphone hears right now, e.g. "Sol3"; null in silence. */
    val heardNoteLabel: String? = null,
    /** -1..1 for -100..+100 cents; null in silence. */
    val needle: Float? = null,
    val tuning: Tuning = Tuning.SILENT,
    val holdSeconds: Double = 0.0,
) {
    val holdProgress: Float get() = (holdSeconds / requiredSeconds).toFloat().coerceIn(0f, 1f)
}

enum class Tuning { SILENT, TOO_LOW, IN_TUNE, TOO_HIGH }

enum class ExercisePhase { READY, LISTENING, SUCCEEDED }

enum class ExerciseError { MICROPHONE_DENIED, MICROPHONE_UNAVAILABLE, SAVE_FAILED }

sealed interface ExerciseEvent {
    /** The screen sends it once the microphone permission is granted. */
    data object StartClicked : ExerciseEvent
    data object StopClicked : ExerciseEvent
    data object PermissionDenied : ExerciseEvent
    data object PlayReferenceClicked : ExerciseEvent
}
