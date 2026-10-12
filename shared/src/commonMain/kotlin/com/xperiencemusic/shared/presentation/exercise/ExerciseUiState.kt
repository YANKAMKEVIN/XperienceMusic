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
    /** An earlier step of the path is not validated yet. */
    val isLocked: Boolean = false,
    /** The daily warm-up: it keeps the streak but has no status. */
    val isWarmUp: Boolean = false,
    val isChallenge: Boolean = false,
)

data class AutomaticCheckUi(
    /** The note to sing now: the only one for a hold, the current one in a sequence. */
    val targetNoteLabel: String,
    val targetMidi: Int,
    /** Hold time per note, rounded for display. */
    val requiredSeconds: Int,
    /** Every note of a sequence, in order; one note for a hold. */
    val targetMidis: List<Int> = listOf(targetMidi),
    val noteLabels: List<String> = listOf(targetNoteLabel),
    val secondsEach: Double = requiredSeconds.toDouble(),
    val currentStep: Int = 0,
    /** The other voice played while the singer holds theirs (harmony), e.g. "Fa#4". */
    val accompanimentMidi: Int? = null,
    val accompanimentLabel: String? = null,
    /** What the microphone hears right now, e.g. "Sol3"; null in silence. */
    val heardNoteLabel: String? = null,
    /** -1..1 for -100..+100 cents; null in silence. */
    val needle: Float? = null,
    val tuning: Tuning = Tuning.SILENT,
    val holdSeconds: Double = 0.0,
) {
    val holdProgress: Float get() = (holdSeconds / secondsEach).toFloat().coerceIn(0f, 1f)

    val isSequence: Boolean get() = targetMidis.size > 1
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
