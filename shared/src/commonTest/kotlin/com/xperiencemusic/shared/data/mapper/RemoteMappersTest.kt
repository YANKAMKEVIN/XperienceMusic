package com.xperiencemusic.shared.data.mapper

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import com.xperiencemusic.shared.data.remote.dto.ProfileDto
import com.xperiencemusic.shared.data.remote.dto.SetlistDto
import com.xperiencemusic.shared.data.remote.dto.SetlistSongDto
import com.xperiencemusic.shared.data.remote.dto.SongDto
import com.xperiencemusic.shared.data.remote.dto.SongLinkDto
import com.xperiencemusic.shared.data.remote.dto.VoiceTrackDto
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.VoicePart
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteMappersTest {
    private val profile = ProfileDto("u1", "g@b.fr", "CONTENT_ADMIN", "Grâce", "SINGER", "ALTO", "PRO")

    @Test
    fun `un profil sans réponses d'onboarding ne donne pas de membre`() {
        assertNull(profile.copy(displayName = null).toDomain())
        assertNull(profile.copy(level = null).toDomain())
    }

    @Test
    fun `un profil complet devient un membre`() {
        val member = profile.toDomain()!!
        assertEquals(Role.CONTENT_ADMIN, member.role)
        assertEquals(Instrument.SINGER, member.profile.instrument)
        assertEquals(VoicePart.ALTO, member.profile.voicePart)
    }

    @Test
    fun `un rôle inconnu ne donne que la lecture`() {
        assertEquals(Role.MEMBER, profile.copy(role = "OWNER").toDomain()!!.role)
    }

    @Test
    fun `un chant garde sa tonalité, trie ses liens et ignore les voix inconnues`() {
        val song = SongDto(
            id = "s1", title = "Chant", keyTonic = 9, keyMode = "MINOR", chart = "[1]",
            voiceTracks = listOf(VoiceTrackDto("v1", "ALTO", "a.m4a"), VoiceTrackDto("v2", "BARITONE", "b.m4a")),
            links = listOf(SongLinkDto("B", "https://b", 2), SongLinkDto("A", "https://a", 1)),
        ).toDomain()
        assertEquals(Key(9, Mode.MINOR), song.key)
        assertEquals(listOf("A", "B"), song.links.map { it.label })
        assertEquals(listOf(VoicePart.ALTO), song.voiceTracks.map { it.voicePart })
    }

    @Test
    fun `une setlist suit l'ordre des positions`() {
        val setlist = SetlistDto(
            "w1", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), null, "Grâce",
            listOf(SetlistSongDto(2, "b"), SetlistSongDto(1, "a")),
        ).toDomain()
        assertEquals(listOf("a", "b"), setlist.songIds)
        assertEquals("Grâce", setlist.publishedBy)
    }

    @Test
    fun `un statut inconnu revient à faire`() {
        assertEquals(ExerciseStatus.VALIDATED, "VALIDATED".toExerciseStatus())
        assertEquals(ExerciseStatus.TODO, "ARCHIVED".toExerciseStatus())
    }
}
