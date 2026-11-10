package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.ActivityAnswer
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningKind
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.nextIn
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PianoLearningTest {
    private val day = LocalDate(2026, 10, 18)
    private val chords = LearningCatalog.module(LearningCatalog.DEFAULT)

    private fun attempt(activity: String, passed: Boolean = true, version: Int = PianoBasics.VERSION) =
        LearningAttempt(activity, "me", activity, version, passed, day)

    @Test
    fun keyboardRequiresExactNotesIncludingOctaveAndRejectsExtras() {
        val activity = PianoBasics.activities.first { it.id == "piano-chords-build-inversion" }
        fun passes(notes: Set<Int>) = ActivityGrader.grade(activity, ActivityAnswer(notes = notes)).passed
        assertTrue(passes(setOf(64, 67, 72)))
        assertFalse(passes(setOf(60, 64, 67)))
        assertFalse(passes(setOf(64, 65, 67, 72)))
    }

    @Test
    fun oldVersionAndFailedAttemptsDoNotUnlockFollowingActivities() {
        val first = PianoBasics.activities.first().id
        assertEquals(0, LearningProgress(listOf(attempt(first, false))).nextIn(chords, emptySet()))
        assertEquals(0, LearningProgress(listOf(attempt(first, version = 0))).nextIn(chords, emptySet()))
        assertEquals(1, LearningProgress(listOf(attempt(first))).nextIn(chords, emptySet()))
    }

    @Test
    fun completionCountsEachActivityOnceAndSchedulesReview() {
        val attempts = PianoBasics.activities.map { attempt(it.id) }
        val progress = LearningProgress(attempts + attempts)
        assertTrue(progress.isComplete)
        assertEquals(PianoBasics.activities.size, progress.completed.size)
        assertFalse(progress.reviewDue(LocalDate(2026, 10, 20)))
        assertTrue(progress.reviewDue(LocalDate(2026, 10, 21)))
    }

    @Test
    fun earExercisesHaveTwoDifferentAnswers() {
        val activities = PianoBasics.activities.filter { it.kind == LearningKind.EAR }
        assertEquals(setOf("Majeur", "Mineur"), activities.map { it.answer }.toSet())
        activities.forEach {
            assertTrue(ActivityGrader.grade(it, ActivityAnswer(choice = it.answer, listened = true)).passed)
            assertFalse(ActivityGrader.grade(it, ActivityAnswer(choice = "incorrect", listened = true)).passed)
        }
    }
}
