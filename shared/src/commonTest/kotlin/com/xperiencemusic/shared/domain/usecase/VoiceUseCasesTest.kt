package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.data.repository.InMemoryVoiceSubmissionRepository
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.fakes.FakeMicrophone
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoiceUseCasesTest {
    private val today = DateProvider { LocalDate(2026, 10, 13) }
    private val singer = FakeSessionRepository(member())
    private val storage = InMemoryAudioStorage()
    private val exercises = InMemoryExerciseRepository(emptyList())
    private val songs = songRepository(song("s1", "À toi la gloire"))
    private val submissions = InMemoryVoiceSubmissionRepository(singer, songs, today)
    private val send = SendVoiceUseCase(singer, storage, submissions, exercises, today)

    @Test
    fun `envoyer range la voix dans le dossier du chantre et compte pour la série`() = runTest {
        assertIs<DomainResult.Success<Unit>>(send("s1", byteArrayOf(1, 2, 3), durationSeconds = 30))
        val sent = submissions.observeMine("me").first().single()
        assertEquals(ExerciseStatus.SUBMITTED, sent.status)
        assertEquals("À toi la gloire", sent.songTitle)
        assertTrue(sent.audioPath.startsWith("submissions/me/s1-"))
        assertEquals(setOf(sent.audioPath), storage.paths)
        assertEquals(setOf(LocalDate(2026, 10, 13)), exercises.observePracticeDays("me").first())
    }

    @Test
    fun `renvoyer remplace l'ancien fichier et redemande un avis`() = runTest {
        send("s1", byteArrayOf(1), 30)
        val first = submissions.observeMine("me").first().single()
        submissions.review(first.id, ExerciseStatus.TO_REWORK, "Le refrain")
        send("s1", byteArrayOf(2), 31)
        val second = submissions.observeMine("me").first().single()
        assertEquals(ExerciseStatus.SUBMITTED, second.status)
        assertNull(second.feedback)
        assertEquals(setOf(second.audioPath), storage.paths, "the old file is deleted")
    }

    @Test
    fun `un enregistrement trop court est refusé avant l'envoi`() = runTest {
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), send("s1", byteArrayOf(1), durationSeconds = 1))
        assertTrue(storage.paths.isEmpty())
    }

    @Test
    fun `seul l'Admin++ répond, et un retour à retravailler demande un commentaire`() = runTest {
        send("s1", byteArrayOf(1), 30)
        val id = submissions.observeMine("me").first().single().id
        assertEquals(
            DomainResult.Failure(DomainError.FORBIDDEN),
            ReviewVoiceUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), submissions)(id, validated = true, feedback = ""),
        )
        val admin = ReviewVoiceUseCase(FakeSessionRepository(member(Role.SUPER_ADMIN)), submissions)
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), admin(id, validated = false, feedback = "  "))
        admin(id, validated = true, feedback = "")
        assertEquals(ExerciseStatus.VALIDATED, submissions.observeAll().first().single().status)
    }

    @Test
    fun `l'enregistreur garde la voix et la rend en WAV`() = runTest {
        val recorder = VoiceRecorder(FakeMicrophone(List(30) { 62 }))
        recorder.start(this)
        testScheduler.advanceUntilIdle()
        assertTrue(recorder.level.value.seconds > 2.0)
        val voice = assertNotNull(recorder.stop())
        assertEquals("RIFF", voice.wav.copyOfRange(0, 4).decodeToString())
        assertEquals(2, voice.durationSeconds)
    }
}
