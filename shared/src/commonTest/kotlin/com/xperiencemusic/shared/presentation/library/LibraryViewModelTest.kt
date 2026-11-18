package com.xperiencemusic.shared.presentation.library

import app.cash.turbine.test
import com.xperiencemusic.shared.data.repository.InMemorySetlistRepository
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveRepertoireUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.recording
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val today = LocalDate(2026, 11, 17)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun setlist(serviceDate: LocalDate, vararg songIds: String) =
        Setlist("setlist-$serviceDate", serviceDate, serviceDate, songIds.toList(), null, "admin")

    private fun viewModel(songs: List<Song>, setlists: List<Setlist> = emptyList()): LibraryViewModel {
        val songRepository = songRepository(*songs.toTypedArray())
        val setlistRepository = InMemorySetlistRepository(setlists)
        return LibraryViewModel(
            SearchSongsUseCase(songRepository),
            ObserveRepertoireUseCase(schedule(setlistRepository, songRepository, today), songRepository) { today },
            ObserveCurrentMemberUseCase(FakeSessionRepository(member(Role.MEMBER))),
            ObserveSyncStatusUseCase(songRepository, setlistRepository),
            RefreshContentUseCase(songRepository, setlistRepository),
        )
    }

    private val catalogue = listOf(
        song("1", "Amazing Grace"),
        song("2", "À toi la gloire"),
        song("3", "Béni soit ton nom"),
        song("4", "10 000 raisons"),
        song("5", "Tu es digne", recordings = listOf(recording("r1", 4, isReference = true))),
        song("6", "Agnus Dei"),
    )

    private fun LibraryUiState.headers() = sections.map { it.header }
    private fun LibraryUiState.ids() = sections.map { section -> section.songs.map { it.id } }

    @Test
    fun `sans répertoire l'écran s'ouvre sur tout le catalogue, rangé par lettre`() = runTest {
        val viewModel = viewModel(catalogue.take(4))
        viewModel.state.test {
            val state = expectMostRecentItem()
            assertEquals(LibraryTab.CATALOGUE, state.tab)
            assertEquals(0, state.repertoireCount)
            assertEquals(4, state.catalogueCount)
            // "À toi" is under A, and what is not a letter closes the index.
            assertEquals(
                listOf("A", "B", "#").map { LibrarySectionHeader.Letter(it) },
                state.headers(),
            )
            assertEquals(listOf(listOf("2", "1"), listOf("3"), listOf("4")), state.ids())
        }
    }

    @Test
    fun `le répertoire montre le prochain service, les chants récents puis tout de A à Z`() = runTest {
        val viewModel = viewModel(
            catalogue,
            listOf(
                setlist(LocalDate(2026, 11, 1), "1", "2"),
                setlist(LocalDate(2026, 11, 8), "2"),
                setlist(LocalDate(2026, 11, 22), "3", "1"),
            ),
        )
        viewModel.state.test {
            val state = expectMostRecentItem()
            assertEquals(LibraryTab.REPERTOIRE, state.tab)
            // Songs 1, 2 and 3 are in a setlist, song 5 has a recording.
            assertEquals(4, state.repertoireCount)
            assertEquals(
                listOf(
                    LibrarySectionHeader.NextService(LocalDate(2026, 11, 22)),
                    LibrarySectionHeader.RecentlySung,
                    LibrarySectionHeader.Alphabetical,
                ),
                state.headers(),
            )
            // Setlist order, then the song sung last (song 1 is already in the next service), then by title.
            assertEquals(listOf(listOf("3", "1"), listOf("2"), listOf("2", "1", "3", "5")), state.ids())
            assertEquals(LocalDate(2026, 11, 8), state.sections[1].songs.single().lastSungOn)
            assertTrue(state.sections[2].songs.last().hasAudio)
        }
    }

    @Test
    fun `la liste de A à Z disparaît quand les sections montrent déjà tout le répertoire`() = runTest {
        val viewModel = viewModel(catalogue.take(2), listOf(setlist(LocalDate(2026, 11, 8), "1", "2")))
        viewModel.state.test {
            assertEquals(listOf<LibrarySectionHeader>(LibrarySectionHeader.RecentlySung), expectMostRecentItem().headers())
        }
    }

    @Test
    fun `on passe d'un onglet à l'autre`() = runTest {
        val viewModel = viewModel(catalogue, listOf(setlist(LocalDate(2026, 11, 8), "1")))
        viewModel.state.test {
            viewModel.onEvent(LibraryEvent.TabSelected(LibraryTab.CATALOGUE))
            val all = expectMostRecentItem()
            assertEquals(LibraryTab.CATALOGUE, all.tab)
            assertEquals(6, all.sections.sumOf { it.songs.size })

            viewModel.onEvent(LibraryEvent.TabSelected(LibraryTab.REPERTOIRE))
            assertEquals(LibraryTab.REPERTOIRE, expectMostRecentItem().tab)
        }
    }

    @Test
    fun `la recherche couvre tout le catalogue, paroles comprises, quel que soit l'onglet`() = runTest {
        val viewModel = viewModel(
            catalogue + song("7", "Psaume 23", chart = "[1]L'Éternel est mon ber[5]ger"),
            listOf(setlist(LocalDate(2026, 11, 8), "1")),
        )
        viewModel.state.test {
            viewModel.onEvent(LibraryEvent.QueryChanged("agnus"))
            val byTitle = expectMostRecentItem()
            assertEquals(listOf<LibrarySectionHeader>(LibrarySectionHeader.Results(1)), byTitle.headers())
            assertEquals(listOf(listOf("6")), byTitle.ids())

            viewModel.onEvent(LibraryEvent.QueryChanged("mon berger"))
            val byLyrics = expectMostRecentItem().sections.single().songs.single()
            assertEquals("7", byLyrics.id)
            assertEquals("L'Éternel est mon berger", byLyrics.lyricLine)

            viewModel.onEvent(LibraryEvent.QueryChanged("introuvable"))
            assertTrue(expectMostRecentItem().sections.isEmpty())
        }
    }
}
