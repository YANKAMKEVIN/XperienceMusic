package com.xperiencemusic.shared.presentation.song

import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.recording
import com.xperiencemusic.shared.fakes.song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongDetailUiStateMapperTest {
    private val sample = song(
        recordings = listOf(recording("old", 1), recording("ref", 2, isReference = true), recording("new", 9)),
        chart = "[1]Gloire [5]à toi",
    )

    @Test
    fun `la référence est listée en premier puis les plus récentes`() {
        val state = deriveSongDetailState(sample, member(), SongDetailLocalState())
        assertEquals(listOf("ref", "new", "old"), state.recordings.map { it.id })
    }

    @Test
    fun `transposer change les accords et signale la tonalité d'origine`() {
        val state = deriveSongDetailState(
            sample,
            member(),
            SongDetailLocalState(transposeSemitones = 2, notation = Notation.SOLFEGE),
        )
        assertEquals("Ré majeur", state.keyLabel)
        assertEquals("Do majeur", state.originalKeyLabel)
        val chords = (state.chart.single() as ChartLine.Lyrics).segments.map { it.chord }
        assertEquals(listOf("Ré", "La"), chords)
    }

    @Test
    fun `en Nashville la tonalité reste écrite en solfège`() {
        val state = deriveSongDetailState(sample, member(), SongDetailLocalState())
        assertEquals("Do majeur", state.keyLabel)
        assertNull(state.originalKeyLabel)
    }

    @Test
    fun `seuls les admins voient la gestion des versions`() {
        assertFalse(deriveSongDetailState(sample, member(Role.MEMBER), SongDetailLocalState()).canManageRecordings)
        assertTrue(deriveSongDetailState(sample, member(Role.CONTENT_ADMIN), SongDetailLocalState()).canManageRecordings)
    }

    @Test
    fun `un chant absent donne l'état introuvable`() {
        assertTrue(deriveSongDetailState(null, member(), SongDetailLocalState()).isMissing)
    }
}
