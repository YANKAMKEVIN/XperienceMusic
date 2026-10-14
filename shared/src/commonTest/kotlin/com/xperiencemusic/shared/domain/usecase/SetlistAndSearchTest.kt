package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.theory.Key
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals("Grâce", setlists.observeSetlists().first().single().publishedBy)
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
        val weekly = schedule(setlists, songs)().first().current
        assertEquals(listOf("s2", "s1"), weekly?.songs?.map { it.id })
    }

    @Test
    fun `sans setlist publiée il n'y a rien`() = runTest {
        assertNull(schedule(setlistRepository(), songRepository())().first().current)
    }

    @Test
    fun `la semaine courante est le prochain dimanche, les suivantes restent à venir`() = runTest {
        val setlists = setlistRepository().apply {
            publish(setlist.copy(rehearsalDate = LocalDate(2026, 10, 6), serviceDate = LocalDate(2026, 10, 11)))
            publish(setlist.copy(rehearsalDate = LocalDate(2026, 10, 20), serviceDate = LocalDate(2026, 10, 25)))
            publish(setlist)
        }
        val schedule = schedule(setlists, songRepository(), today = LocalDate(2026, 10, 12))().first()
        assertEquals(LocalDate(2026, 10, 18), schedule.current?.setlist?.serviceDate)
        assertEquals(listOf(LocalDate(2026, 10, 18), LocalDate(2026, 10, 25)), schedule.ahead.map { it.setlist.serviceDate })
        assertEquals(3, schedule.weeks.size, "past weeks stay readable")
    }

    @Test
    fun `sans semaine à venir la dernière reste affichée`() = runTest {
        val setlists = setlistRepository().apply { publish(setlist) }
        val schedule = schedule(setlists, songRepository(), today = LocalDate(2026, 10, 30))().first()
        assertEquals(LocalDate(2026, 10, 18), schedule.current?.setlist?.serviceDate)
    }

    @Test
    fun `un chant d'une semaine à venir suit l'ordre de cette semaine`() = runTest {
        val setlists = setlistRepository().apply {
            publish(setlist.copy(songIds = listOf("s1")))
            publish(setlist.copy(rehearsalDate = LocalDate(2026, 10, 20), serviceDate = LocalDate(2026, 10, 25), songIds = listOf("s1", "s2")))
        }
        val songs = songRepository(song("s1"), song("s2"))
        val schedule = schedule(setlists, songs)().first()
        assertEquals(LocalDate(2026, 10, 18), schedule.weekOf("s1")?.setlist?.serviceDate, "the current week comes first")
        assertEquals(LocalDate(2026, 10, 25), schedule.weekOf("s2")?.setlist?.serviceDate)
    }

    @Test
    fun `republier un dimanche remplace sa setlist`() = runTest {
        val setlists = setlistRepository().apply {
            publish(setlist)
            publish(setlist.copy(songIds = listOf("s1")))
        }
        assertEquals(listOf("s1"), setlists.observeSetlists().first().single().songIds)
    }

    @Test
    fun `seuls les admins suppriment une semaine`() = runTest {
        val setlists = setlistRepository().apply { publish(setlist) }
        assertEquals(
            DomainResult.Failure(DomainError.FORBIDDEN),
            DeleteSetlistUseCase(FakeSessionRepository(member(Role.MEMBER)), setlists)(setlist.serviceDate),
        )
        DeleteSetlistUseCase(FakeSessionRepository(member(Role.CONTENT_ADMIN)), setlists)(setlist.serviceDate)
        assertTrue(setlists.observeSetlists().first().isEmpty())
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
    fun `la recherche peut se limiter à une tonalité`() = runTest {
        val songs = songRepository(
            song("1", "Amazing Grace", key = Key(7)),
            song("2", "À toi la gloire", key = Key(2)),
            song("3", "Agnus Dei", key = Key(7)),
        )
        val search = SearchSongsUseCase(songs)
        assertEquals(listOf("3", "1"), search("", Key(7)).first().map { it.id })
        assertEquals(listOf("1"), search("grace", Key(7)).first().map { it.id })
        assertTrue(search("grace", Key(2)).first().isEmpty())
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
