package com.xperiencemusic.shared.domain.learning

import kotlinx.datetime.LocalDate
import kotlin.test.*

class PianoLearningTest {
    private val day = LocalDate(2026, 10, 18)
    private fun attempt(activity: String, passed: Boolean = true, version: Int = PianoBasics.VERSION) =
        LearningAttempt(activity, "me", activity, version, passed, day)

    @Test fun keyboardRequiresExactNotesIncludingOctaveAndRejectsExtras() {
        val activity = PianoBasics.activities.first { it.id == "piano-chords-build-inversion" }
        assertTrue(PianoBasics.check(activity, setOf(64, 67, 72), null))
        assertFalse(PianoBasics.check(activity, setOf(60, 64, 67), null))
        assertFalse(PianoBasics.check(activity, setOf(64, 65, 67, 72), null))
    }

    @Test fun oldVersionAndFailedAttemptsDoNotUnlockFollowingActivities() {
        val first = PianoBasics.activities.first().id
        assertEquals(0, LearningProgress(listOf(attempt(first, false))).nextIndex)
        assertEquals(0, LearningProgress(listOf(attempt(first, version = 0))).nextIndex)
        assertEquals(1, LearningProgress(listOf(attempt(first))).nextIndex)
    }

    @Test fun completionCountsEachActivityOnceAndSchedulesReview() {
        val attempts = PianoBasics.activities.map { attempt(it.id) }
        val progress = LearningProgress(attempts + attempts)
        assertTrue(progress.isComplete)
        assertEquals(PianoBasics.activities.size, progress.completed.size)
        assertFalse(progress.reviewDue(LocalDate(2026, 10, 20)))
        assertTrue(progress.reviewDue(LocalDate(2026, 10, 21)))
    }

    @Test fun earExercisesHaveTwoDifferentAnswers() {
        val activities = PianoBasics.activities.filter { it.kind == LearningKind.EAR }
        assertEquals(setOf("Majeur", "Mineur"), activities.map { it.answer }.toSet())
        activities.forEach {
            assertTrue(PianoBasics.check(it, emptySet(), it.answer))
            assertFalse(PianoBasics.check(it, emptySet(), "incorrect"))
        }
    }
}
