package com.xperiencemusic.shared.domain.model

/** What the member gave for an activity: only the fields of its kind matter. */
data class ActivityAnswer(
    val notes: Set<Int> = emptySet(),
    val choice: String? = null,
    /** An ear question is answered only after hearing the example. */
    val listened: Boolean = false,
    /** Tap times, in seconds, for a rhythm activity. */
    val taps: List<Double> = emptyList(),
    /** The microphone heard the sung notes held long enough. */
    val sung: Boolean = false,
)

/** How a wrong chord differs from the expected one, from the most precise advice to the most general. */
sealed interface KeyboardCorrection {
    /** The selected notes that belong to the chord. */
    val correctCount: Int

    /** [wrong] sits a semitone away from the missing [target]. */
    data class MoveNote(override val correctCount: Int, val wrong: Int, val target: Int) : KeyboardCorrection

    /** [wrong] has the right name in the wrong octave. */
    data class CheckOctave(override val correctCount: Int, val wrong: Int) : KeyboardCorrection

    data class RemoveExtra(override val correctCount: Int, val extra: List<Int>) : KeyboardCorrection

    data class FindMissing(override val correctCount: Int, val missingCount: Int) : KeyboardCorrection
}

/** The verdict on an activity, with what the member needs to hear about it. */
sealed interface GradeResult {
    val passed: Boolean

    /** A lesson read: always a success. */
    data object LessonDone : GradeResult {
        override val passed = true
    }

    data object Passed : GradeResult {
        override val passed = true
    }

    /** The taps drifted from the beat by [averageError] of a beat on average. */
    data class OffTempo(val averageError: Double) : GradeResult {
        override val passed = false
    }

    data class WrongNotes(val correction: KeyboardCorrection) : GradeResult {
        override val passed = false
    }

    /** A wrong answer to an ear question; [choseColour] when the question was "major or minor?". */
    data class WrongHearing(val choice: String, val choseColour: Boolean) : GradeResult {
        override val passed = false
    }

    data object TryAgain : GradeResult {
        override val passed = false
    }
}

/** How sending a take for an objective ended, when the take itself reached the server. */
enum class SubmissionOutcome {
    /** The take and the evaluation request were both sent. */
    REVIEW_REQUESTED,

    /** The take is kept on the server but the evaluation request failed: the member can ask again. */
    TAKE_KEPT,
}
