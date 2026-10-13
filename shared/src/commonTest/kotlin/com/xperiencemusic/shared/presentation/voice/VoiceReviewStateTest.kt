package com.xperiencemusic.shared.presentation.voice

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class VoiceReviewStateTest {
    private fun submission(id: String, day: Int, status: ExerciseStatus) = VoiceSubmission(
        id, "m$id", "Membre $id", VoicePart.ALTO, "s", "Chant", "p/$id.wav", 65, LocalDate(2026, 10, day), status, null,
    )

    @Test
    fun `les envois en attente passent en premier, du plus ancien au plus récent`() {
        val state = deriveVoiceReviewState(
            listOf(
                submission("a", 12, ExerciseStatus.SUBMITTED),
                submission("b", 10, ExerciseStatus.VALIDATED),
                submission("c", 11, ExerciseStatus.SUBMITTED),
            ),
            ReviewLocal(),
            PlaybackState(track = PlaybackTrack("voice-c", "Membre c", "Chant", "p/c.wav"), isPlaying = true),
        )
        assertEquals(listOf("c", "a"), state.toReview.map { it.id })
        assertEquals(listOf("b"), state.reviewed.map { it.id })
        assertEquals(PlaybackStatus.PLAYING, state.toReview.first().playback)
        assertEquals("1:05", state.toReview.first().duration)
    }
}
