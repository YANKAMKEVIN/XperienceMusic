package com.xperiencemusic.shared.presentation.player

import com.xperiencemusic.shared.domain.model.PlaybackState
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.presentation.common.PlaybackStatus
import com.xperiencemusic.shared.presentation.common.statusOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class MiniPlayerUiStateTest {
    private val track = PlaybackTrack("r1", "Répétition", "À toi la gloire", "p")

    @Test
    fun `rien de chargé, rien d'affiché`() {
        assertFalse(PlaybackState().toMiniPlayerUiState().isVisible)
    }

    @Test
    fun `la progression et les durées sont formatées`() {
        val ui = PlaybackState(track, isPlaying = true, positionMs = 85_000, durationMs = 212_000).toMiniPlayerUiState()
        assertEquals("1:25", ui.positionLabel)
        assertEquals("3:32", ui.durationLabel)
        assertEquals(85f / 212f, ui.progress, 0.001f)
    }

    @Test
    fun `dans une setlist le lecteur dit où on en est`() {
        val second = PlaybackState(track, isPlaying = true, queueIndex = 1, queueSize = 5).toMiniPlayerUiState()
        assertEquals("2 / 5", second.queuePosition)
        assertTrue(second.hasPrevious)
        assertTrue(second.hasNext)

        val alone = PlaybackState(track, isPlaying = true, queueSize = 1).toMiniPlayerUiState()
        assertNull(alone.queuePosition)
        assertFalse(alone.hasNext)
    }

    @Test
    fun `le statut de lecture de chaque élément`() {
        val playing = PlaybackState(track, isPlaying = true)
        assertEquals(PlaybackStatus.PLAYING, playing.statusOf("r1"))
        assertEquals(PlaybackStatus.IDLE, playing.statusOf("r2"))
        assertEquals(PlaybackStatus.PAUSED, playing.copy(isPlaying = false).statusOf("r1"))
        assertEquals(PlaybackStatus.LOADING, playing.copy(isLoading = true).statusOf("r1"))
        assertEquals(PlaybackStatus.IDLE, playing.copy(hasError = true).statusOf("r1"))
    }
}
