package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.domain.model.AudioFile
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.recording
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UploadRecordingUseCaseTest {
    private val audio = AudioFile("repet.M4A", "audio/mp4", ByteArray(10) { 1 }, durationSeconds = null)
    private val storage = InMemoryAudioStorage()
    private val sessions = FakeSessionRepository(member(Role.CONTENT_ADMIN))

    private fun upload(songs: SongRepository) = UploadRecordingUseCase(sessions, songs, storage, AddRecordingUseCase(sessions, songs))

    @Test
    fun `envoie le fichier puis ajoute la version`() = runTest {
        val songs = songRepository(song("s1"))
        val result = upload(songs)("s1", " Répétition ", LocalDate(2026, 10, 13), audio, makeReference = false, replace = null)
        val added = assertIs<DomainResult.Success<*>>(result).let { songs.song("s1")!!.recordings.single() }
        assertEquals("Répétition", added.title)
        assertEquals(1, added.durationSeconds, "an unknown duration is stored as 1 s")
        assertTrue(added.isReference, "the first recording becomes the reference")
        assertTrue(added.audioPath.startsWith("songs/s1/") && added.audioPath.endsWith(".m4a"))
        assertEquals(setOf(added.audioPath), storage.paths)
    }

    @Test
    fun `un chant complet sans remplacement ne déclenche aucun envoi`() = runTest {
        val full = song("s1", recordings = listOf(recording("a", 1, true), recording("b", 2), recording("c", 3)))
        val result = upload(songRepository(full))("s1", "D", LocalDate(2026, 10, 13), audio, false, null)
        assertEquals(DomainResult.Failure(DomainError.RECORDING_LIMIT_REACHED), result)
        assertTrue(storage.paths.isEmpty())
    }

    @Test
    fun `un fichier trop lourd est refusé avant l'envoi`() = runTest {
        val heavy = AudioFile("big.wav", "audio/wav", ByteArray(AudioFile.MAX_BYTES + 1), 600)
        val result = upload(songRepository(song("s1")))("s1", "Big", LocalDate(2026, 10, 13), heavy, false, null)
        assertEquals(DomainResult.Failure(DomainError.FILE_TOO_LARGE), result)
        assertTrue(storage.paths.isEmpty())
    }

    @Test
    fun `si l'ajout échoue après l'envoi, le fichier est supprimé`() = runTest {
        val songs = object : SongRepository by songRepository(song("s1")) {
            override suspend fun addRecording(songId: String, recording: Recording, replacingId: String?) =
                throw DomainException(DomainError.NETWORK)
        }
        val result = upload(songs)("s1", "A", LocalDate(2026, 10, 13), audio, false, null)
        assertEquals(DomainResult.Failure(DomainError.NETWORK), result)
        assertTrue(storage.paths.isEmpty())
    }

    @Test
    fun `un membre simple ne peut rien envoyer`() = runTest {
        val songs = songRepository(song("s1"))
        val memberOnly = FakeSessionRepository(member(Role.MEMBER))
        val result = UploadRecordingUseCase(memberOnly, songs, storage, AddRecordingUseCase(memberOnly, songs))(
            "s1", "A", LocalDate(2026, 10, 13), audio, false, null,
        )
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), result)
        assertTrue(storage.paths.isEmpty())
    }
}
