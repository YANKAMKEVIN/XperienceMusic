package com.xperiencemusic.shared.presentation

import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.fakes.FakeAudioCache
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import com.xperiencemusic.shared.presentation.library.LibraryEvent
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.presentation.week.WeekEvent
import com.xperiencemusic.shared.presentation.week.WeekViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * First launch with no network: the phone has no copy of the songs yet, so the repository
 * publishes nothing. The screens used to wait on it forever, behind a spinner and without the
 * offline banner; they now say the load failed and offer to retry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirstLoadOfflineTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** Songs that only arrive with a successful refresh, like the Supabase repository without a snapshot. */
    private class NoCopyYet(status: SyncStatus, private val catalogue: List<Song>) : SongRepository by songRepository() {
        private val copy = MutableStateFlow<List<Song>?>(null)
        override val syncStatus = MutableStateFlow(status)
        var isOnline = false

        override fun observeSongs(): Flow<List<Song>> = copy.filterNotNull()

        override suspend fun refresh() {
            syncStatus.value = SyncStatus.SYNCING
            if (!isOnline) {
                syncStatus.value = SyncStatus.FAILED
                error("offline")
            }
            copy.value = catalogue
            syncStatus.value = SyncStatus.UP_TO_DATE
        }

        fun publishCopy() {
            copy.value = catalogue
        }
    }

    private fun library(songs: SongRepository): LibraryViewModel {
        val setlists = setlistRepository()
        return LibraryViewModel(
            SearchSongsUseCase(songs),
            ObserveCurrentMemberUseCase(FakeSessionRepository(member(Role.MEMBER))),
            ObserveSyncStatusUseCase(songs, setlists),
            RefreshContentUseCase(songs, setlists),
        )
    }

    private fun week(songs: SongRepository): WeekViewModel {
        val setlists = setlistRepository()
        val schedule = schedule(setlists, songs)
        val cache = FakeAudioCache()
        return WeekViewModel(
            schedule,
            ObserveCurrentMemberUseCase(FakeSessionRepository(member(Role.MEMBER))),
            ObserveSetlistOfflineUseCase(schedule, cache),
            DownloadSetlistUseCase(schedule, cache),
            ObserveSyncStatusUseCase(songs, setlists),
            RefreshContentUseCase(songs, setlists),
            PlaybackController(FakeAudioPlayer(), { it }, CoroutineScope(UnconfinedTestDispatcher())),
            GetTodayUseCase { LocalDate(2026, 10, 10) },
        )
    }

    private val catalogue = listOf(song("1", "Amazing Grace"))

    @Test
    fun `sans réseau ni copie, Chants dit que le chargement a échoué au lieu de tourner sans fin`() = runTest {
        val songs = NoCopyYet(SyncStatus.FAILED, catalogue)
        val viewModel = library(songs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        val failed = viewModel.state.value
        assertTrue(failed.hasLoadFailed)
        assertFalse(failed.isLoading)
        assertFalse(failed.isOffline, "no copy on screen: the offline banner would describe nothing")

        songs.isOnline = true
        viewModel.onEvent(LibraryEvent.RefreshPulled)
        val loaded = viewModel.state.value
        assertFalse(loaded.hasLoadFailed)
        assertFalse(loaded.isLoading)
        assertEquals(listOf("1"), loaded.songs.map { it.id })
    }

    @Test
    fun `pendant la première synchronisation, Chants affiche le chargement`() = runTest {
        val viewModel = library(NoCopyYet(SyncStatus.SYNCING, catalogue))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        assertTrue(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.hasLoadFailed)
    }

    @Test
    fun `avec une copie sur le téléphone, l'échec reste le bandeau hors connexion`() = runTest {
        val songs = NoCopyYet(SyncStatus.FAILED, catalogue).apply { publishCopy() }
        val viewModel = library(songs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        assertTrue(viewModel.state.value.isOffline)
        assertFalse(viewModel.state.value.hasLoadFailed)
        assertEquals(1, viewModel.state.value.songs.size)
    }

    @Test
    fun `sans réseau ni copie, Semaine dit que le chargement a échoué et réessaie`() = runTest {
        val songs = NoCopyYet(SyncStatus.FAILED, catalogue)
        val viewModel = week(songs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

        val failed = viewModel.state.value
        assertTrue(failed.hasLoadFailed)
        assertFalse(failed.isLoading)
        assertFalse(failed.isOffline)

        songs.isOnline = true
        viewModel.onEvent(WeekEvent.RefreshPulled)
        val loaded = viewModel.state.value
        assertFalse(loaded.hasLoadFailed)
        assertFalse(loaded.isLoading)
    }
}
