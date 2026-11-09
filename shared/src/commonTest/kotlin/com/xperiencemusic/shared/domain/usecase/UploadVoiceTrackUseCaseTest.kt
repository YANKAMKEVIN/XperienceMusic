package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.domain.model.AudioFile
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UploadVoiceTrackUseCaseTest {
    private val songs = songRepository(song("s1", "À toi la gloire"))
    private val storage = InMemoryAudioStorage()
    private val audio = AudioFile("alto.mp3", "audio/mpeg", ByteArray(10), durationSeconds = 30)
    private fun upload(role: Role) = UploadVoiceTrackUseCase(FakeSessionRepository(member(role)), songs, storage)

    @Test
    fun `une nouvelle prise remplace la piste de la même voix et son fichier`() = runTest {
        val upload = upload(Role.CONTENT_ADMIN)
        assertIs<DomainResult.Success<Unit>>(upload("s1", VoicePart.ALTO, audio))
        val first = songs.song("s1")!!.voiceTracks.single().audioPath

        assertIs<DomainResult.Success<Unit>>(upload("s1", VoicePart.ALTO, audio))

        val track = songs.song("s1")!!.voiceTracks.single()
        assertEquals(setOf(track.audioPath), storage.paths)
        assert(track.audioPath != first)
    }

    @Test
    fun `les voix différentes coexistent`() = runTest {
        val upload = upload(Role.CONTENT_ADMIN)
        upload("s1", VoicePart.ALTO, audio)
        upload("s1", VoicePart.TENOR, audio)
        assertEquals(setOf(VoicePart.ALTO, VoicePart.TENOR), songs.song("s1")!!.voiceTracks.map { it.voicePart }.toSet())
    }

    @Test
    fun `un membre ne peut pas déposer de piste`() = runTest {
        val result = upload(Role.MEMBER)("s1", VoicePart.ALTO, audio)
        assertEquals(DomainError.FORBIDDEN, assertIs<DomainResult.Failure>(result).error)
        assertEquals(emptySet(), storage.paths)
    }
}
