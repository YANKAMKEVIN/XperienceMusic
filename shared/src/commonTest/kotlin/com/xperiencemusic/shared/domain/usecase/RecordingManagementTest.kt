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

class RecordingManagementTest {
    private val songWithThree = song(
        recordings = listOf(recording("a", 1, isReference = true), recording("b", 5), recording("c", 3)),
    )

    @Test
    fun `supprimer la référence promeut la version la plus récente`() = runTest {
        val repo = songRepository(songWithThree)
        DeleteRecordingUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), repo)("s1", "a")
        val recordings = repo.song("s1")!!.recordings
        assertEquals(listOf("b", "c"), recordings.map { it.id })
        assertEquals("b", recordings.single { it.isReference }.id)
    }

    @Test
    fun `un membre simple ne peut pas supprimer`() = runTest {
        val repo = songRepository(songWithThree)
        val result = DeleteRecordingUseCase(FakeSessionRepository(member(Role.MEMBER)), repo)("s1", "a")
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), result)
        assertEquals(3, repo.song("s1")!!.recordings.size)
    }

    @Test
    fun `sans session on ne peut rien modifier`() = runTest {
        val result = DeleteRecordingUseCase(FakeSessionRepository(null), songRepository(songWithThree))("s1", "a")
        assertEquals(DomainResult.Failure(DomainError.NOT_SIGNED_IN), result)
    }

    @Test
    fun `définir une référence la rend unique`() = runTest {
        val repo = songRepository(songWithThree)
        SetReferenceRecordingUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), repo)("s1", "c")
        assertEquals(listOf(false, false, true), repo.song("s1")!!.recordings.map { it.isReference })
    }

    @Test
    fun `les rôles héritent des droits attendus`() {
        assertEquals(true, Role.SUPER_ADMIN.permissions.containsAll(Role.CONTENT_ADMIN.permissions))
        assertEquals(true, Role.MEMBER.permissions.isEmpty())
    }
}
