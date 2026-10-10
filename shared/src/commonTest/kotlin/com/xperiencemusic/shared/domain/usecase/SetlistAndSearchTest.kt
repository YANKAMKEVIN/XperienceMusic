package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SetlistAndSearchTest {
    private val setlist = Setlist(
        id = "w1",
        rehearsalDate = LocalDate(2026, 10, 13),
        serviceDate = LocalDate(2026, 10, 18),
        songIds = listOf("s2", "missing", "s1"),
        message = null,
        publishedBy = "",
    )

    @Test
    fun `seuls les admins publient la setlist`() = runTest {
        val result = PublishSetlistUseCase(FakeSessionRepository(member(Role.MEMBER)), setlistRepository())(setlist)
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), result)
    }

    @Test
    fun `une setlist publiée porte le nom de l'admin`() = runTest {
        val setlists = setlistRepository()
        val result = PublishSetlistUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), setlists)(setlist)
        assertIs<DomainResult.Success<Setlist>>(result)
        assertEquals("Grâce", setlists.observeLatest().first()?.publishedBy)
    }

    @Test
    fun `une setlist dont la prestation précède la répétition est refusée`() = runTest {
        val invalid = setlist.copy(serviceDate = LocalDate(2026, 10, 12))
        val result = PublishSetlistUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), setlistRepository())(invalid)
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), result)
    }

    @Test
    fun `la setlist de la semaine suit l'ordre publié et ignore les chants inconnus`() = runTest {
        val setlists = setlistRepository().apply { publish(setlist) }
        val songs = songRepository(song("s1", "Premier"), song("s2", "Second"))
        val weekly = ObserveWeeklySetlistUseCase(setlists, songs)().first()
        assertEquals(listOf("s2", "s1"), weekly?.songs?.map { it.id })
    }

    @Test
    fun `sans setlist publiée il n'y a rien`() = runTest {
        assertNull(ObserveWeeklySetlistUseCase(setlistRepository(), songRepository())().first())
    }

    @Test
    fun `la recherche ignore la casse et les accents et trie par titre`() = runTest {
        val songs = songRepository(
            song("1", "Ésprit saint"),
            song("2", "Amazing Grace", artist = "John Newton"),
            song("3", "À toi la gloire"),
        )
        val search = SearchSongsUseCase(songs)
        assertEquals(listOf("3", "2", "1"), search("").first().map { it.id })
        assertEquals(listOf("1"), search("esprit").first().map { it.id })
        assertEquals(listOf("2"), search("NEWTON").first().map { it.id })
    }

    @Test
    fun `l'onboarding refuse un prénom vide et retire la voix des musiciens`() = runTest {
        val sessions = FakeSessionRepository()
        val onboard = CompleteOnboardingUseCase(sessions)
        val blank = MemberProfile("  ", Instrument.SINGER, VoicePart.ALTO, Level.PRO)
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), onboard(blank))

        onboard(MemberProfile(" Daniel ", Instrument.PIANO, VoicePart.TENOR, Level.PRO))
        val saved = sessions.currentMember()!!.profile
        assertEquals("Daniel", saved.displayName)
        assertNull(saved.voicePart)
    }
}
