package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningKind
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.model.dailyPlan
import com.xperiencemusic.shared.domain.model.reviewDate
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LearningExperienceTest {
    private fun attempt(day: Int, passed: Boolean = true, hints: Int = 0) =
        LearningAttempt("$day-$passed-$hints", "me", PianoBasics.REVIEW_ID, 1, passed, LocalDate(2026, 10, day), hints = hints)

    private fun reviewDate(vararg attempts: LearningAttempt) = LearningProgress(attempts.toList()).reviewDate(PianoBasics.REVIEW_ID)

    @Test
    fun revisionsExpandOnlyAfterUnassistedSuccessesOnDifferentDays() {
        assertEquals(LocalDate(2026, 10, 21), reviewDate(attempt(18)))
        assertEquals(LocalDate(2026, 10, 21), reviewDate(attempt(18), attempt(18)))
        assertEquals(LocalDate(2026, 10, 26), reviewDate(attempt(18), attempt(19)))
        assertEquals(LocalDate(2026, 10, 21), reviewDate(attempt(18), attempt(19), attempt(20, false)))
        assertEquals(LocalDate(2026, 10, 21), reviewDate(attempt(20, hints = 1)))
    }

    @Test
    fun variantsKeepCorrectIntervalsAndStayInsideKeyboard() {
        PianoBasics.activities.filter { it.kind == LearningKind.KEYBOARD }.forEach { base ->
            for (n in 1..15) {
                val varied = variedActivity(base, n)
                assertEquals(
                    base.expectedNotes.sorted().zipWithNext { a, b -> b - a },
                    varied.expectedNotes.sorted().zipWithNext { a, b -> b - a },
                )
                assertTrue(varied.expectedNotes.all { it in 60..84 })
            }
        }
        val ear = PianoBasics.activities.first { it.kind == LearningKind.EAR }
        assertEquals(setOf("Majeur", "Mineur"), (1..8).map { variedActivity(ear, it).answer }.toSet())
    }

    @Test
    fun modulesHaveDistinctActivitiesAndObjectiveLinks() {
        assertEquals(5, LearningCatalog.modulesFor(Instrument.PIANO).size)
        val activities = LearningCatalog.modules.flatMap { it.activities }
        assertEquals(activities.size, activities.map { it.id }.distinct().size)
        assertEquals(LearningCatalog.modules.size, LearningCatalog.modules.map { it.objectiveId }.distinct().size)
        val plan = LearningProgress().dailyPlan(LearningCatalog.modules.first(), LocalDate(2026, 10, 18))
        assertEquals(listOf(0, 1, 2), plan)
    }

    @Test
    fun noteLabelsNameTheOctaveAsTheLessonsDo() {
        assertEquals("Do4", noteLabel(60))
        assertEquals("Mi♭4", noteLabel(63))
        assertEquals("Do5", noteLabel(72))
    }
}
