package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.ExerciseProgress
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.SingerPath
import com.xperiencemusic.shared.domain.model.StepState
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.fakes.member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressUiStateMapperTest {
    private val hold = Exercise("e1", 1, 1, "Quinte", "", ExerciseCheck.HoldNote(7, 5.0))
    private val scale = Exercise("e2", 1, 2, "Gamme", "", ExerciseCheck.NoteSequence(listOf(0, 2, 4, 5, 7), 1.0), isChallenge = true)
    private val next = Exercise("e3", 2, 1, "Octave", "", ExerciseCheck.HoldNote(12, 3.0))

    private fun path(vararg statuses: ExerciseStatus) = SingerPath.of(
        listOf(hold, scale, next).zip(statuses.toList()) { exercise, status -> ExerciseProgress(exercise, status) },
        warmUp = null,
        streakDays = 4,
    )

    @Test
    fun `la note cible dépend de la voix du chantre`() {
        val p = path(ExerciseStatus.VALIDATED, ExerciseStatus.TODO, ExerciseStatus.TODO)
        val tenor = deriveProgressState(member(voicePart = VoicePart.TENOR), p)
        val soprano = deriveProgressState(member(voicePart = VoicePart.SOPRANO), p)
        assertEquals("Ré4 · 5 s", tenor.tiers.first().steps.first().detail) // Sol3 + quinte
        assertEquals("Ré5 · 5 s", soprano.tiers.first().steps.first().detail) // Sol4 + quinte
        assertEquals("5 notes", tenor.tiers.first().steps[1].detail)
        assertEquals(4, tenor.streakDays)
    }

    @Test
    fun `seul le premier exercice non validé est ouvert, les paliers suivent`() {
        val state = deriveProgressState(member(), path(ExerciseStatus.VALIDATED, ExerciseStatus.VALIDATED, ExerciseStatus.TODO))
        assertEquals(listOf(1, 2), state.tiers.map { it.number })
        assertTrue(state.tiers.first().isComplete)
        assertEquals(1, state.completedTiers)
        assertEquals(StepState.CURRENT, state.tiers[1].steps.single().state)

        val start = deriveProgressState(member(), path(ExerciseStatus.TODO, ExerciseStatus.TODO, ExerciseStatus.TODO))
        assertEquals(listOf(StepState.CURRENT, StepState.LOCKED), start.tiers.first().steps.map { it.state })
        assertEquals(StepState.LOCKED, start.tiers[1].steps.single().state)
    }

    @Test
    fun `les musiciens n'ont pas encore de parcours`() {
        val state = deriveProgressState(member(instrument = Instrument.DRUMS, voicePart = null), path(ExerciseStatus.TODO, ExerciseStatus.TODO, ExerciseStatus.TODO))
        assertFalse(state.hasPath)
        assertEquals(emptyList(), state.tiers)
    }
}
