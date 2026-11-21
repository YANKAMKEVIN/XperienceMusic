package com.xperiencemusic.shared.presentation.library

import app.cash.turbine.test
import com.xperiencemusic.shared.data.repository.InMemorySetlistRepository
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveRepertoireUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.LazyChartsSongRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.fakes.song
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

/** The list comes without the lyrics: a search long enough to read them fetches them, once. */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryLyricsSearchTest {
    private val today = LocalDate(2026, 11, 17)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val songs = LazyChartsSongRepository(
        song("1", "Amazing Grace", chart = "[1]Amazing grace, how sweet the sound"),
        song("2", "Tu es digne", chart = "[1]Tu es digne, Sei[4]gneur, de recevoir la gloire"),
    )

    private fun viewModel(): LibraryViewModel {
        val setlists = InMemorySetlistRepository(emptyList())
        return LibraryViewModel(
            SearchSongsUseCase(songs),
            ObserveRepertoireUseCase(schedule(setlists, songs, today), songs) { today },
            ObserveCurrentMemberUseCase(FakeSessionRepository(member(Role.MEMBER))),
            ObserveSyncStatusUseCase(songs, setlists),
            RefreshContentUseCase(songs, setlists),
        )
    }

    private fun LibraryUiState.found() = sections.flatMap { it.songs }.map { it.title to it.lyricLine }

    @Test
    fun `les paroles ne se chargent qu'à partir de 3 lettres, et une seule fois`() = runTest {
        val viewModel = viewModel()
        viewModel.state.test {
            viewModel.onEvent(LibraryEvent.QueryChanged("se"))
            assertEquals(0, songs.lyricsLoads)

            viewModel.onEvent(LibraryEvent.QueryChanged("sei"))
            viewModel.onEvent(LibraryEvent.QueryChanged("seig"))
            viewModel.onEvent(LibraryEvent.QueryChanged("seigneur"))

            assertEquals(1, songs.lyricsLoads)
            assertEquals(listOf("Tu es digne" to "Tu es digne, Seigneur, de recevoir la gloire"), expectMostRecentItem().found())
        }
    }

    @Test
    fun `sans réseau, les titres restent trouvés et la recherche suivante réessaie`() = runTest {
        songs.isOffline = true
        val viewModel = viewModel()
        viewModel.state.test {
            viewModel.onEvent(LibraryEvent.QueryChanged("grace"))
            assertEquals(listOf("Amazing Grace" to null), expectMostRecentItem().found())

            songs.isOffline = false
            viewModel.onEvent(LibraryEvent.QueryChanged("gloire"))

            assertEquals(2, songs.lyricsLoads)
            assertEquals(listOf("Tu es digne"), expectMostRecentItem().found().map { it.first })
        }
    }
}
