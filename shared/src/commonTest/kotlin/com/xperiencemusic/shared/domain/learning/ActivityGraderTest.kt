package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.ActivityAnswer
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.KeyboardCorrection
import com.xperiencemusic.shared.domain.model.LearningActivity
import com.xperiencemusic.shared.domain.model.LearningKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActivityGraderTest {
    private val allActivities = LearningCatalog.modules.flatMap { it.activities }
    private fun first(kind: LearningKind, withChoices: Boolean? = null) = allActivities.first {
        it.kind == kind && (withChoices == null || it.choices.isNotEmpty() == withChoices)
    }
    private val cMinor = PianoBasics.activities.first { it.id == "piano-chords-build-minor" }

    @Test
    fun aLessonIsDoneOnceReadAndAPracticeOnceDeclared() {
        assertEquals(GradeResult.LessonDone, ActivityGrader.grade(first(LearningKind.LESSON), ActivityAnswer()))
        assertEquals(GradeResult.Passed, ActivityGrader.grade(first(LearningKind.PRACTICE), ActivityAnswer()))
    }

    @Test
    fun eachKindWaitsForItsOwnAnswerBeforeChecking() {
        assertFalse(ActivityGrader.canSubmit(cMinor, ActivityAnswer()))
        assertTrue(ActivityGrader.canSubmit(cMinor, ActivityAnswer(notes = setOf(60))))
        val ear = first(LearningKind.EAR)
        assertFalse(ActivityGrader.canSubmit(ear, ActivityAnswer(choice = "Majeur")), "an ear question is heard first")
        assertTrue(ActivityGrader.canSubmit(ear, ActivityAnswer(choice = "Majeur", listened = true)))
        assertTrue(ActivityGrader.canSubmit(first(LearningKind.CHOICE), ActivityAnswer(choice = "2")))
        val rhythm = first(LearningKind.RHYTHM)
        assertFalse(ActivityGrader.canSubmit(rhythm, ActivityAnswer(taps = List(7) { it.toDouble() })))
        assertTrue(ActivityGrader.canSubmit(rhythm, ActivityAnswer(taps = List(8) { it.toDouble() })))
        val sing = first(LearningKind.SING)
        assertFalse(ActivityGrader.canSubmit(sing, ActivityAnswer()))
        assertTrue(ActivityGrader.canSubmit(sing, ActivityAnswer(sung = true)))
        assertTrue(ActivityGrader.canSubmit(first(LearningKind.LESSON), ActivityAnswer()))
    }

    @Test
    fun onlyTheMicrophonePassesASungActivity() {
        val sing = first(LearningKind.SING)
        assertFalse(ActivityGrader.grade(sing, ActivityAnswer()).passed)
        assertTrue(ActivityGrader.grade(sing, ActivityAnswer(sung = true)).passed)
    }

    @Test
    fun aSteadyTempoPassesAndAnUnevenOneSaysByHowMuch() {
        val rhythm = first(LearningKind.RHYTHM)
        val beat = 60.0 / rhythm.bpm
        assertEquals(GradeResult.Passed, ActivityGrader.grade(rhythm, ActivityAnswer(taps = List(8) { it * beat })))
        val rushed = List(8) { it * beat * 0.5 }
        val result = assertIs<GradeResult.OffTempo>(ActivityGrader.grade(rhythm, ActivityAnswer(taps = rushed)))
        assertEquals(0.5, result.averageError, 0.001)
        assertTrue(result.averageError > ActivityGrader.RHYTHM_TOLERANCE)
    }

    @Test
    fun rhythmMeasuresIntervalsAndRejectsIncompleteAttempts() {
        assertNull(ActivityGrader.rhythmError(listOf(0.0, 1.0), 60))
        assertEquals(0.0, ActivityGrader.rhythmError((0..7).map { it.toDouble() }, 60)!!, 0.001)
    }

    @Test
    fun aWrongChordNamesTheNoteToMove() {
        val result = assertIs<GradeResult.WrongNotes>(ActivityGrader.grade(cMinor, ActivityAnswer(notes = setOf(60, 64, 67))))
        assertEquals(KeyboardCorrection.MoveNote(correctCount = 2, wrong = 64, target = 63), result.correction)
    }

    @Test
    fun aWrongChordAdvisesFromTheMostPreciseToTheMostGeneral() {
        fun correction(notes: Set<Int>) =
            assertIs<GradeResult.WrongNotes>(ActivityGrader.grade(cMinor, ActivityAnswer(notes = notes))).correction
        assertEquals(KeyboardCorrection.CheckOctave(correctCount = 2, wrong = 75), correction(setOf(60, 67, 75)))
        assertEquals(KeyboardCorrection.RemoveExtra(correctCount = 3, extra = listOf(70, 72)), correction(setOf(60, 63, 67, 70, 72)))
        assertEquals(KeyboardCorrection.FindMissing(correctCount = 1, missingCount = 2), correction(setOf(60)))
    }

    @Test
    fun aWrongHearingSaysWhetherTheQuestionWasMajorOrMinor() {
        val colour = first(LearningKind.EAR, withChoices = false)
        val listed = first(LearningKind.EAR, withChoices = true)
        assertEquals(GradeResult.WrongHearing("Faux", choseColour = true), ActivityGrader.grade(colour, ActivityAnswer(choice = "Faux")))
        assertEquals(GradeResult.WrongHearing("Faux", choseColour = false), ActivityGrader.grade(listed, ActivityAnswer(choice = "Faux")))
        assertEquals(GradeResult.TryAgain, ActivityGrader.grade(first(LearningKind.CHOICE), ActivityAnswer(choice = "Faux")))
    }

    @Test
    fun everyKindIsGraded() {
        LearningKind.entries.forEach { kind ->
            val activity = allActivities.firstOrNull { it.kind == kind } ?: LearningActivity("x", "x", kind, "", "")
            ActivityGrader.grade(activity, ActivityAnswer())
        }
    }
}
