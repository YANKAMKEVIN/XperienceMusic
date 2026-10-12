package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.OfflineAvailability
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.model.VoiceTrack
import com.xperiencemusic.shared.fakes.FakeAudioCache
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.recording
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfflineUseCasesTest {
    private val songs = songRepository(
        song("s1", recordings = listOf(recording("a", 1, true), recording("b", 2)))
            .copy(voiceTracks = listOf(VoiceTrack("v", VoicePart.ALTO, "demo://v"))),
        song("s2", recordings = listOf(recording("c", 3, true))),
        song("other", recordings = listOf(recording("z", 4, true))),
    )
    private val cache = FakeAudioCache(setOf("demo://a"))

    private suspend fun weekly(): ObserveSetlistScheduleUseCase {
        val setlists = setlistRepository().apply {
            publish(Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("s1", "s2"), null, "x"))
        }
        return schedule(setlists, songs)
    }

    @Test
    fun `la semaine compte les versions et pistes de voix de ses chants seulement`() = runTest {
        val availability = ObserveSetlistOfflineUseCase(weekly(), cache)().first()
        assertEquals(OfflineAvailability(available = 1, total = 4), availability)
    }

    @Test
    fun `télécharger ne reprend que ce qui manque`() = runTest {
        val result = DownloadSetlistUseCase(weekly(), cache)()
        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(listOf("demo://b", "demo://v", "demo://c"), cache.downloaded)
        assertTrue(ObserveSetlistOfflineUseCase(weekly(), cache)().first().isComplete)
    }

    @Test
    fun `une coupure arrête le téléchargement et la reprise continue`() = runTest {
        cache.failing += "demo://v"
        assertEquals(DomainResult.Failure(DomainError.NETWORK), DownloadSetlistUseCase(weekly(), cache)())
        assertEquals(listOf("demo://b"), cache.downloaded)

        cache.failing.clear()
        DownloadSetlistUseCase(weekly(), cache)()
        assertEquals(listOf("demo://b", "demo://v", "demo://c"), cache.downloaded)
    }

    @Test
    fun `se déconnecter efface les données du téléphone même sans réseau`() = runTest {
        var cleared = false
        val sessions = FakeSessionRepository(member(Role.MEMBER)).apply { failure = IllegalStateException("offline") }
        val result = SignOutUseCase(sessions) { cleared = true }()
        assertTrue(cleared)
        assertEquals(DomainResult.Failure(DomainError.NETWORK), result)
    }
}
