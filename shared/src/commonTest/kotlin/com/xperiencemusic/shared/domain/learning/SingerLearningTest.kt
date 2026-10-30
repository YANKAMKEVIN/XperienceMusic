package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.model.Instrument
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
}
