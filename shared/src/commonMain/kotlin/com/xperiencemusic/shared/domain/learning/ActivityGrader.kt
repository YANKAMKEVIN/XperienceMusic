package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.ActivityAnswer
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.KeyboardCorrection
import com.xperiencemusic.shared.domain.model.LearningActivity
import com.xperiencemusic.shared.domain.model.LearningKind
import kotlin.math.abs

/** The single place that decides when an activity can be checked and whether it passed. */
object ActivityGrader {
    /** Average distance to the beat, as a share of it, still counted as a steady tempo. */
    const val RHYTHM_TOLERANCE = 0.18

    /** Taps needed before the tempo is measured. */
    const val RHYTHM_TAPS = 8

    fun canSubmit(activity: LearningActivity, answer: ActivityAnswer): Boolean = when (activity.kind) {
        LearningKind.KEYBOARD -> answer.notes.isNotEmpty()
        LearningKind.EAR -> answer.listened && answer.choice != null
        LearningKind.CHOICE -> answer.choice != null
        LearningKind.RHYTHM -> answer.taps.size >= RHYTHM_TAPS
        LearningKind.SING -> answer.sung
        LearningKind.LESSON, LearningKind.PRACTICE -> true
    }

    fun grade(activity: LearningActivity, answer: ActivityAnswer): GradeResult = when (activity.kind) {
        LearningKind.LESSON -> GradeResult.LessonDone
        LearningKind.PRACTICE -> GradeResult.Passed
        LearningKind.KEYBOARD -> if (answer.notes == activity.expectedNotes) {
            GradeResult.Passed
        } else {
            GradeResult.WrongNotes(correction(activity.expectedNotes, answer.notes))
        }
        LearningKind.EAR -> if (answer.choice == activity.answer) {
            GradeResult.Passed
        } else {
            GradeResult.WrongHearing(answer.choice.orEmpty(), choseColour = activity.choices.isEmpty())
        }
        LearningKind.CHOICE -> if (answer.choice == activity.answer) GradeResult.Passed else GradeResult.TryAgain
        // Timing is evaluated from the tap intervals, never a checkbox.
        LearningKind.RHYTHM -> {
            val error = rhythmError(answer.taps, activity.bpm)
            when {
                error == null -> GradeResult.TryAgain
                error <= RHYTHM_TOLERANCE -> GradeResult.Passed
                else -> GradeResult.OffTempo(error)
            }
        }
        // Only the microphone passes a sung activity.
        LearningKind.SING -> if (answer.sung) GradeResult.Passed else GradeResult.TryAgain
    }

    /** Average gap between the taps and the beat, as a share of the beat; null before enough taps. */
    fun rhythmError(taps: List<Double>, bpm: Int): Double? {
        if (taps.size < RHYTHM_TAPS) return null
        val period = 60.0 / bpm
        return taps.zipWithNext { a, b -> abs((b - a) - period) / period }.average()
    }

    private fun correction(expected: Set<Int>, notes: Set<Int>): KeyboardCorrection {
        val correct = notes.intersect(expected).size
        val extra = notes - expected
        val missing = expected - notes
        val wrong = extra.firstOrNull()
        val target = missing.firstOrNull { right -> wrong != null && abs(right - wrong) <= 1 }
        return when {
            wrong != null && target != null -> KeyboardCorrection.MoveNote(correct, wrong, target)
            wrong != null && missing.any { it % 12 == wrong % 12 } -> KeyboardCorrection.CheckOctave(correct, wrong)
            extra.isNotEmpty() -> KeyboardCorrection.RemoveExtra(correct, extra.sorted())
            else -> KeyboardCorrection.FindMissing(correct, missing.size)
        }
    }
}
