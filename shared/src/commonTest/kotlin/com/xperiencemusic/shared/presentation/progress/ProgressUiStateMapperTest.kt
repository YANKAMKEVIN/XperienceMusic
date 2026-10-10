package com.xperiencemusic.shared.presentation.progress

import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.ExerciseProgress
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.fakes.member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProgressUiStateMapperTest {
    private val hold = Exercise("e1", Level.BEGINNER, 1, "Quinte", "", ExerciseCheck.HoldNote(7, 5.0))
    private val review = Exercise("e2", Level.BEGINNER, 2, "Gamme", "", ExerciseCheck.Review)

    @Test
    fun `la note cible dépend de la voix du chantre`() {
        val path = listOf(ExerciseProgress(hold, ExerciseStatus.VALIDATED), ExerciseProgress(review, ExerciseStatus.TODO))
        val tenor = deriveProgressState(member(voicePart = VoicePart.TENOR), path)
        val soprano = deriveProgressState(member(voicePart = VoicePart.SOPRANO), path)
        assertEquals(ExerciseCheckUi.HoldNote("Ré4", 5), tenor.exercises.first().check) // Sol3 + quinte
        assertEquals(ExerciseCheckUi.HoldNote("Ré5", 5), soprano.exercises.first().check) // Sol4 + quinte
        assertEquals(1, tenor.validatedCount)
    }

    @Test
    fun `les musiciens n'ont pas encore de parcours`() {
        val state = deriveProgressState(member(instrument = Instrument.DRUMS, voicePart = null), emptyList())
        assertFalse(state.hasPath)
        assertEquals(emptyList(), state.exercises)
    }
}
