package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.model.Instrument
import kotlinx.datetime.LocalDate
import kotlin.test.*

class SingerLearningTest {
    private val singers = LearningCatalog.modulesFor(Instrument.SINGER)

    @Test fun everyObjectiveWithoutAMicrophoneTierHasAModule() {
        val expected = listOf("c4", "c5", "c6", "c7", "p3", "p4", "p5", "p6", "p7").map { "singer-$it" }
        assertEquals(expected, singers.map { it.objectiveId })
        val known = GrowthObjectives.all.map { it.id }.toSet()
        assertTrue(singers.all { it.objectiveId in known })
    }

    @Test fun pianoModulesStayApart() {
        assertTrue(LearningCatalog.modulesFor(Instrument.PIANO).none { it.instrument == Instrument.SINGER })
        assertEquals(LearningCatalog.DEFAULT, LearningCatalog.defaultFor(Instrument.PIANO)?.id)
        assertNull(LearningCatalog.defaultFor(Instrument.DRUMS))
    }

    @Test fun activityIdsAreUnique() {
        val ids = LearningCatalog.modules.flatMap { it.activities }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun answersAreAmongTheChoices() {
        singers.flatMap { it.activities }.filter { it.choices.isNotEmpty() }.forEach {
            assertTrue(it.answer in it.choices, it.id)
        }
    }

    @Test fun sungActivitiesHaveTargetsAndOnlyTheMicrophonePassesThem() {
        val sung = singers.flatMap { it.activities }.filter { it.kind == LearningKind.SING }
        assertTrue(sung.isNotEmpty())
        sung.forEach {
            val target = assertNotNull(it.sing, it.id)
            assertTrue(target.semitones.isNotEmpty() && target.secondsEach > 0, it.id)
            assertFalse(PianoBasics.check(it, emptySet(), null), it.id)
        }
    }

    @Test fun contentStaysOffTheWeeklySchedule() {
        val text = singers.flatMap { m -> m.activities.flatMap { listOf(it.title, it.instruction, it.explanation) + it.choices } }
            .joinToString(" ").lowercase()
        listOf("setlist", "dimanche", "vendredi", "samedi").forEach { assertFalse(it in text, it) }
    }

    @Test fun singerModulesGetASungChallenge() {
        singers.forEach { assertTrue(it.challenge().title.contains(it.title), it.id) }
    }

    private val hold = LearningCatalog.module("singer-hold")
    private val firstSung = hold.activities.indexOfFirst { it.kind == LearningKind.SING }
    private fun passed(vararg indices: Int) = LearningProgress(indices.map {
        LearningAttempt("a$it", "me", hold.activities[it].id, PianoBasics.VERSION, true, LocalDate(2026, 10, 30))
    })

    @Test fun onlySungActivitiesCanWait() {
        assertTrue(hold.activities[firstSung].canPostpone())
        assertTrue(LearningCatalog.modules.flatMap { it.activities }.filter { it.canPostpone() }.all { it.kind == LearningKind.SING })
    }

    @Test fun aSungActivitySetAsideOpensTheNextStepWithoutCountingAsDone() {
        val progress = passed(*(0 until firstSung).toList().toIntArray())
        val aside = setOf(hold.activities[firstSung].id)
        assertEquals(firstSung, progress.nextIn(hold))
        assertEquals(firstSung + 1, progress.nextIn(hold, aside))
        assertFalse(hold.activities[firstSung].id in progress.completedIn(hold))
        assertFalse(progress.complete(hold))
        assertFalse(firstSung in progress.dailyPlan(hold, LocalDate(2026, 10, 30), aside))
    }

    @Test fun whenOnlySetAsideActivitiesRemainTheMemberComesBackToThem() {
        val others = hold.activities.indices.filter { it != firstSung }
        val progress = passed(*others.toIntArray())
        val aside = setOf(hold.activities[firstSung].id)
        assertEquals(firstSung, progress.nextIn(hold, aside))
        assertTrue(firstSung in progress.dailyPlan(hold, LocalDate(2026, 10, 30), aside))
    }
}
