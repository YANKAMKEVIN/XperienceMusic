package com.xperiencemusic.shared.ui.learning

import androidx.compose.runtime.Composable
import com.xperiencemusic.shared.domain.learning.noteLabel
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.KeyboardCorrection
import com.xperiencemusic.shared.domain.model.LearningKind
import com.xperiencemusic.shared.domain.model.Mastery
import com.xperiencemusic.shared.presentation.learning.CheckFeedback
import com.xperiencemusic.shared.presentation.learning.LearningError
import com.xperiencemusic.shared.presentation.learning.MicrophoneIssue
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.learning_correct_notes
import com.xperiencemusic.shared.resources.learning_error_review
import com.xperiencemusic.shared.resources.learning_error_save
import com.xperiencemusic.shared.resources.learning_feedback_ear
import com.xperiencemusic.shared.resources.learning_feedback_ear_colour
import com.xperiencemusic.shared.resources.learning_feedback_extra
import com.xperiencemusic.shared.resources.learning_feedback_lesson
import com.xperiencemusic.shared.resources.learning_feedback_missing
import com.xperiencemusic.shared.resources.learning_feedback_move
import com.xperiencemusic.shared.resources.learning_feedback_octave
import com.xperiencemusic.shared.resources.learning_feedback_passed
import com.xperiencemusic.shared.resources.learning_feedback_passed_help
import com.xperiencemusic.shared.resources.learning_feedback_retry
import com.xperiencemusic.shared.resources.learning_feedback_tempo
import com.xperiencemusic.shared.resources.learning_kind_choice
import com.xperiencemusic.shared.resources.learning_kind_ear
import com.xperiencemusic.shared.resources.learning_kind_keyboard
import com.xperiencemusic.shared.resources.learning_kind_lesson
import com.xperiencemusic.shared.resources.learning_kind_practice_piano
import com.xperiencemusic.shared.resources.learning_kind_practice_singer
import com.xperiencemusic.shared.resources.learning_kind_rhythm
import com.xperiencemusic.shared.resources.learning_kind_sing
import com.xperiencemusic.shared.resources.learning_mastery_consolidating
import com.xperiencemusic.shared.resources.learning_mastery_consolidating_text
import com.xperiencemusic.shared.resources.learning_mastery_discover
import com.xperiencemusic.shared.resources.learning_mastery_discover_text
import com.xperiencemusic.shared.resources.learning_mastery_practising
import com.xperiencemusic.shared.resources.learning_mastery_practising_text
import com.xperiencemusic.shared.resources.learning_mastery_submitted
import com.xperiencemusic.shared.resources.learning_mastery_submitted_text
import com.xperiencemusic.shared.resources.learning_mastery_validated
import com.xperiencemusic.shared.resources.learning_mastery_validated_text
import com.xperiencemusic.shared.resources.learning_mic_denied
import com.xperiencemusic.shared.resources.learning_mic_unavailable
import com.xperiencemusic.shared.resources.learning_note_count
import com.xperiencemusic.shared.resources.learning_seconds
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** "Mi4 · Sol4 · Do5". */
internal fun notesLabel(notes: Iterable<Int>) = notes.joinToString(" · ") { noteLabel(it) }

/** "5 s", "2,5 s": the French decimal comma. */
@Composable
internal fun secondsLabel(seconds: Double): String {
    val value = if (seconds == seconds.toInt().toDouble()) "${seconds.toInt()}" else seconds.toString().replace('.', ',')
    return stringResource(Res.string.learning_seconds, value)
}

@Composable
internal fun Mastery.label(): String = stringResource(
    when (this) {
        Mastery.DISCOVER -> Res.string.learning_mastery_discover
        Mastery.SUBMITTED -> Res.string.learning_mastery_submitted
        Mastery.PRACTISING -> Res.string.learning_mastery_practising
        Mastery.CONSOLIDATING -> Res.string.learning_mastery_consolidating
        Mastery.VALIDATED -> Res.string.learning_mastery_validated
    },
)

@Composable
internal fun Mastery.explanation(): String = stringResource(
    when (this) {
        Mastery.DISCOVER -> Res.string.learning_mastery_discover_text
        Mastery.SUBMITTED -> Res.string.learning_mastery_submitted_text
        Mastery.PRACTISING -> Res.string.learning_mastery_practising_text
        Mastery.CONSOLIDATING -> Res.string.learning_mastery_consolidating_text
        Mastery.VALIDATED -> Res.string.learning_mastery_validated_text
    },
)

/** The eyebrow of an activity card. */
@Composable
internal fun LearningKind.eyebrow(instrument: Instrument): String = stringResource(
    when (this) {
        LearningKind.LESSON -> Res.string.learning_kind_lesson
        LearningKind.EAR -> Res.string.learning_kind_ear
        LearningKind.KEYBOARD -> Res.string.learning_kind_keyboard
        LearningKind.PRACTICE -> if (instrument == Instrument.SINGER) {
            Res.string.learning_kind_practice_singer
        } else {
            Res.string.learning_kind_practice_piano
        }
        LearningKind.CHOICE -> Res.string.learning_kind_choice
        LearningKind.RHYTHM -> Res.string.learning_kind_rhythm
        LearningKind.SING -> Res.string.learning_kind_sing
    },
)

@Composable
internal fun LearningError.text(): String = stringResource(
    when (this) {
        LearningError.SAVE_FAILED -> Res.string.learning_error_save
        LearningError.REVIEW_NOT_SENT -> Res.string.learning_error_review
    },
)

@Composable
internal fun MicrophoneIssue.text(): String = stringResource(
    when (this) {
        MicrophoneIssue.DENIED -> Res.string.learning_mic_denied
        MicrophoneIssue.UNAVAILABLE -> Res.string.learning_mic_unavailable
    },
)

/** What the member reads under "Vérifier". */
@Composable
internal fun CheckFeedback.text(): String = when (val graded = result) {
    GradeResult.LessonDone -> stringResource(Res.string.learning_feedback_lesson)
    GradeResult.Passed -> stringResource(if (hints > 0) Res.string.learning_feedback_passed_help else Res.string.learning_feedback_passed)
    is GradeResult.OffTempo -> stringResource(Res.string.learning_feedback_tempo, (graded.averageError * 100).toInt())
    is GradeResult.WrongNotes -> graded.correction.text()
    is GradeResult.WrongHearing -> stringResource(
        if (graded.choseColour) Res.string.learning_feedback_ear_colour else Res.string.learning_feedback_ear,
        graded.choice,
    )
    GradeResult.TryAgain -> stringResource(Res.string.learning_feedback_retry)
}

@Composable
private fun KeyboardCorrection.text(): String {
    val correct = pluralStringResource(Res.plurals.learning_correct_notes, correctCount, correctCount)
    return when (this) {
        is KeyboardCorrection.MoveNote -> stringResource(Res.string.learning_feedback_move, correct, noteLabel(wrong), noteLabel(target))
        is KeyboardCorrection.CheckOctave -> stringResource(Res.string.learning_feedback_octave, noteLabel(wrong))
        is KeyboardCorrection.RemoveExtra -> stringResource(Res.string.learning_feedback_extra, correct, notesLabel(extra))
        is KeyboardCorrection.FindMissing -> stringResource(
            Res.string.learning_feedback_missing,
            correct,
            pluralStringResource(Res.plurals.learning_note_count, missingCount, missingCount),
        )
    }
}
