package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import kotlinx.datetime.LocalDate
import kotlin.test.*

class LearningJourneyTest {
    private val today=LocalDate(2026,10,18)
    private val module=LearningCatalog.modules.first()
    private fun attempt(id:String,day:Int)=LearningAttempt(id,"me",module.activities.first().id,PianoBasics.VERSION,true,LocalDate(2026,10,day))
    @Test fun onlyResponsibleValidationGrantsValidatedMastery() {
        val completed=LearningProgress(module.activities.map { LearningAttempt(it.id,"me",it.id,PianoBasics.VERSION,true,today) })
        assertEquals(Mastery.CONSOLIDATING,completed.mastery(module,null,today))
        assertEquals(Mastery.VALIDATED,completed.mastery(module,ExerciseStatus.VALIDATED,today))
        assertEquals(Mastery.CONSOLIDATING,LearningProgress().mastery(module,ExerciseStatus.TO_REWORK,today))
        assertEquals(Mastery.DISCOVER,LearningProgress().mastery(module,null,today))
    }
    @Test fun weeklyGoalCountsDistinctDaysAndIgnoresFutureAndOldDates() {
        val p=LearningProgress(listOf(attempt("1",12),attempt("2",12),attempt("3",18),attempt("4",11),attempt("5",19)))
        assertEquals(2,p.weeklyDays(today))
        assertFalse(p.returning(today))
        assertTrue(LearningProgress(listOf(attempt("1",11))).returning(today))
        assertFalse(LearningProgress().returning(today))
    }
    @Test fun everyModuleHasActionableChallengeAndPlayableDemonstration() {
        LearningCatalog.modules.forEach {
            assertEquals(3,it.challenge().steps.size)
            assertTrue(it.challenge().criteria.isNotBlank())
            assertTrue(it.challenge().example.all { note -> note in 60..84 })
        }
    }
}
