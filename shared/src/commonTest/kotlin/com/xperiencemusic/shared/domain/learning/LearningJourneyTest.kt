package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.Mastery
import com.xperiencemusic.shared.domain.model.mastery
import com.xperiencemusic.shared.domain.model.returning
import com.xperiencemusic.shared.domain.model.sessionPlan
import com.xperiencemusic.shared.domain.model.weeklyDays
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearningJourneyTest {
    private val today = LocalDate(2026, 10, 18)
    private val module = LearningCatalog.modules.first()

    private fun attempt(id: String, day: Int) =
        LearningAttempt(id, "me", module.activities.first().id, PianoBasics.VERSION, true, LocalDate(2026, 10, day))

    @Test
    fun onlyResponsibleValidationGrantsValidatedMastery() {
        val completed = LearningProgress(module.activities.map { LearningAttempt(it.id, "me", it.id, PianoBasics.VERSION, true, today) })
        assertEquals(Mastery.CONSOLIDATING, completed.mastery(module, null, today))
        assertEquals(Mastery.VALIDATED, completed.mastery(module, ExerciseStatus.VALIDATED, today))
        assertEquals(Mastery.CONSOLIDATING, LearningProgress().mastery(module, ExerciseStatus.TO_REWORK, today))
        assertEquals(Mastery.DISCOVER, LearningProgress().mastery(module, null, today))
        assertEquals(Mastery.SUBMITTED, LearningProgress().mastery(module, ExerciseStatus.SUBMITTED, today))
    }

    @Test
    fun assistanceAndErrorsStayVisibleBeforeCompletingModule() {
        val failed = attempt("error", 18).copy(passed = false)
        assertEquals(Mastery.CONSOLIDATING, LearningProgress(listOf(failed)).mastery(module, null, today))
        assertEquals(Mastery.CONSOLIDATING, LearningProgress(listOf(failed.copy(passed = true, hints = 1))).mastery(module, null, today))
        assertEquals(
            Mastery.PRACTISING,
            LearningProgress(listOf(failed, failed.copy(id = "retry", passed = true))).mastery(module, null, today),
        )
    }

    @Test
    fun weeklyGoalCountsDistinctDaysAndIgnoresFutureAndOldDates() {
        val progress = LearningProgress(listOf(attempt("1", 12), attempt("2", 12), attempt("3", 18), attempt("4", 11), attempt("5", 19)))
        assertEquals(2, progress.weeklyDays(today))
        assertFalse(progress.returning(today))
        assertTrue(LearningProgress(listOf(attempt("1", 11))).returning(today))
        assertFalse(LearningProgress().returning(today))
    }

    @Test
    fun aMemberComingBackAfterABreakStartsWithASingleActivity() {
        assertEquals(listOf(1, 2, 3), LearningProgress(listOf(attempt("1", 17))).sessionPlan(module, today, emptySet()))
        assertEquals(1, LearningProgress(listOf(attempt("1", 10))).sessionPlan(module, today, emptySet()).size)
    }

    @Test
    fun everyModuleHasActionableChallengeAndPlayableDemonstration() {
        LearningCatalog.modules.forEach {
            assertEquals(3, it.challenge().steps.size)
            assertTrue(it.challenge().criteria.isNotBlank())
            assertTrue(it.challenge().example.all { note -> note in 60..84 })
        }
    }
}
