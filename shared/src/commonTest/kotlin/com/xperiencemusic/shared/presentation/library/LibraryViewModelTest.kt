package com.xperiencemusic.shared.presentation.library

import app.cash.turbine.test
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import com.xperiencemusic.shared.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg songs: Song): LibraryViewModel {
        val songRepository = songRepository(*songs)
        val setlists = setlistRepository()
        return LibraryViewModel(
            SearchSongsUseCase(songRepository),
            ObserveCurrentMemberUseCase(FakeSessionRepository(member(Role.MEMBER))),
            ObserveSyncStatusUseCase(songRepository, setlists),
            RefreshContentUseCase(songRepository, setlists),
        )
    }

    @Test
    fun `les tonalités du catalogue vont des majeurs aux mineurs, en partant de Do`() {
        val keys = deriveKeyFilters(
            listOf(
                song("1", key = Key(9, Mode.MINOR)),
                song("2", key = Key(7)),
                song("3", key = Key(2)),
                song("4", key = Key(7)),
            ),
        )
        assertEquals(listOf(Key(2), Key(7), Key(9, Mode.MINOR)), keys)
    }

    @Test
    fun `choisir une tonalité ne garde que ses chants`() = runTest {
        val viewModel = viewModel(
            song("1", "Amazing Grace", key = Key(7)),
            song("2", "À toi la gloire", key = Key(2)),
        )
        viewModel.state.test {
            assertEquals(listOf("Ré", "Sol"), expectMostRecentItem().keys)

            viewModel.onEvent(LibraryEvent.KeySelected("Sol"))
            val filtered = expectMostRecentItem()
            assertEquals("Sol", filtered.selectedKey)
            assertEquals(listOf("1"), filtered.songs.map { it.id })

            viewModel.onEvent(LibraryEvent.KeySelected(null))
            assertEquals(listOf("2", "1"), expectMostRecentItem().songs.map { it.id })
        }
    }

    @Test
    fun `le filtre se cache quand tous les chants sont dans la même tonalité`() = runTest {
        val viewModel = viewModel(song("1", key = Key(7)), song("2", key = Key(7)))
        viewModel.state.test {
            viewModel.onEvent(LibraryEvent.KeySelected("Sol"))
            val state = expectMostRecentItem()
            assertTrue(state.keys.isEmpty())
            assertNull(state.selectedKey)
            assertEquals(2, state.songs.size)
        }
    }
}
