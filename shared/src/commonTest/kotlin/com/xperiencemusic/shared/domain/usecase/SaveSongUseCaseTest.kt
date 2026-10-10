package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SaveSongUseCaseTest {
    private val draft = SongDraft("  Saint  ", "  ", Key(7), "[Sol]Saint [Do]saint\n")

    @Test
    fun `un membre simple ne peut pas créer de chant`() = runTest {
        val save = SaveSongUseCase(FakeSessionRepository(member(Role.MEMBER)), songRepository())
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), save(null, draft))
    }

    @Test
    fun `le titre est obligatoire`() = runTest {
        val save = SaveSongUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), songRepository())
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), save(null, draft.copy(title = "   ")))
    }

    @Test
    fun `un nouveau chant est nettoyé et stocké en chiffres`() = runTest {
        val songs = songRepository()
        val result = SaveSongUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), songs)(null, draft)
        val id = assertIs<DomainResult.Success<String>>(result).value
        val saved = songs.song(id)!!
        assertEquals("Saint", saved.title)
        assertNull(saved.artist)
        assertEquals("[1]Saint [4]saint", saved.chart)
    }

    @Test
    fun `modifier un chant garde ses versions`() = runTest {
        val existing = song(id = "s1", recordings = listOf(com.xperiencemusic.shared.fakes.recording("r", 1, true)))
        val songs = songRepository(existing)
        SaveSongUseCase(FakeSessionRepository(member(Role.SUPER_ADMIN)), songs)("s1", draft)
        val saved = songs.song("s1")!!
        assertEquals("Saint", saved.title)
        assertEquals(Key(7), saved.key)
        assertEquals(1, saved.recordings.size)
    }
}
