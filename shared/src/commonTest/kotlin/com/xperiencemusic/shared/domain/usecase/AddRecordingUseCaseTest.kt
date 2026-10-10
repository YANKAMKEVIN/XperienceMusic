package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.recording
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AddRecordingUseCaseTest {
    private val fullSong = song(
        recordings = listOf(recording("a", 1, isReference = true), recording("b", 2), recording("c", 3)),
    )

    private fun useCase(role: Role, vararg songs: com.xperiencemusic.shared.domain.model.Song) =
        songRepository(*songs).let { repo -> AddRecordingUseCase(FakeSessionRepository(member(role)), repo) to repo }

    @Test
    fun `un membre simple ne peut pas ajouter de version`() = runTest {
        val (addRecording, _) = useCase(Role.MEMBER, song())
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), addRecording("s1", recording("new", 4)))
    }

    @Test
    fun `la première version devient la référence`() = runTest {
        val (addRecording, repo) = useCase(Role.CONTENT_ADMIN, song())
        addRecording("s1", recording("new", 4))
        assertEquals(listOf(true), repo.song("s1")!!.recordings.map { it.isReference })
    }

    @Test
    fun `une quatrième version est refusée sans remplacement`() = runTest {
        val (addRecording, _) = useCase(Role.CONTENT_ADMIN, fullSong)
        assertEquals(
            DomainResult.Failure(DomainError.RECORDING_LIMIT_REACHED),
            addRecording("s1", recording("new", 4)),
        )
    }

    @Test
    fun `remplacer une alternative garde la référence`() = runTest {
        val (addRecording, repo) = useCase(Role.CONTENT_ADMIN, fullSong)
        assertIs<DomainResult.Success<*>>(addRecording("s1", recording("new", 4), replace = "b"))
        val recordings = repo.song("s1")!!.recordings
        assertEquals(listOf("a", "c", "new"), recordings.map { it.id })
        assertEquals("a", recordings.single { it.isReference }.id)
    }

    @Test
    fun `remplacer la référence transmet le statut à la nouvelle version`() = runTest {
        val (addRecording, repo) = useCase(Role.SUPER_ADMIN, fullSong)
        addRecording("s1", recording("new", 4), replace = "a")
        assertEquals("new", repo.song("s1")!!.recordings.single { it.isReference }.id)
    }

    @Test
    fun `remplacer une version inconnue échoue`() = runTest {
        val (addRecording, _) = useCase(Role.CONTENT_ADMIN, fullSong)
        assertEquals(DomainResult.Failure(DomainError.NOT_FOUND), addRecording("s1", recording("new", 4), replace = "zz"))
    }
}
