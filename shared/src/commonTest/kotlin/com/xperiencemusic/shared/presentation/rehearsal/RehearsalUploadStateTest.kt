package com.xperiencemusic.shared.presentation.rehearsal

import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceTrack
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.fakes.song
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RehearsalUploadStateTest {
    private val rehearsal = LocalDate(2026, 11, 10)
    private val sunday = LocalDate(2026, 11, 15)
    private fun recording(on: LocalDate, title: String) = Recording(title, title, on, 200, "a/$title", isReference = true)
    private val clean = song("clean", "Version existante", recordings = listOf(recording(LocalDate(2026, 3, 1), "Version propre")))
    private val fresh = song("fresh", "Nouveau", recordings = listOf(recording(rehearsal, "Répétition")))
        .copy(voiceTracks = listOf(VoiceTrack("t", VoicePart.TENOR, "t"), VoiceTrack("s", VoicePart.SOPRANO, "s")))
    private val bare = song("bare", "Sans audio")
    private val week = WeeklySetlist(
        Setlist("w", rehearsal, sunday, listOf("clean", "fresh", "bare"), null, "x"),
        listOf(clean, fresh, bare),
    )
    private val schedule = SetlistSchedule(listOf(week), 0)

    @Test
    fun `un chant avec une version propre garde sa référence sans nouvelle prise`() {
        val row = deriveRehearsalUpload(schedule, sunday).songs.first()
        assertEquals("Version propre", row.referenceTitle)
        assertFalse(row.hasNewTake)
    }

    @Test
    fun `une prise de cette répétition et les voix sont signalées`() {
        val row = deriveRehearsalUpload(schedule, sunday).songs[1]
        assertTrue(row.hasNewTake)
        assertEquals(listOf(VoicePart.SOPRANO, VoicePart.TENOR), row.voiceParts)
    }

    @Test
    fun `un chant sans enregistrement n'a pas de référence`() {
        assertNull(deriveRehearsalUpload(schedule, sunday).songs[2].referenceTitle)
    }

    @Test
    fun `une semaine inconnue donne un écran vide`() {
        val state = deriveRehearsalUpload(schedule, LocalDate(2026, 12, 6))
        assertFalse(state.isLoading)
        assertTrue(state.songs.isEmpty())
    }
}
