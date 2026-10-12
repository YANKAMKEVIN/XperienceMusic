package com.xperiencemusic.shared.presentation.week

import app.cash.turbine.test
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.schedule
import kotlinx.coroutines.CoroutineScope
import com.xperiencemusic.shared.fakes.FakeAudioCache
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.recording
import com.xperiencemusic.shared.fakes.setlistRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModelTest {
    private val cache = FakeAudioCache()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `télécharger la semaine la rend disponible hors connexion`() = runTest {
        val songs = songRepository(song("s1", recordings = listOf(recording("a", 1, true), recording("b", 2))))
        val setlists = setlistRepository().apply {
            publish(Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("s1"), null, "x"))
        }
        val schedule = schedule(setlists, songs)
        val viewModel = WeekViewModel(
            schedule,
            ObserveCurrentMemberUseCase(FakeSessionRepository(member(Role.MEMBER))),
            ObserveSetlistOfflineUseCase(schedule, cache),
            DownloadSetlistUseCase(schedule, cache),
            ObserveSyncStatusUseCase(songs, setlists),
            RefreshContentUseCase(songs, setlists),
            PlaybackController(FakeAudioPlayer(), { it }, CoroutineScope(UnconfinedTestDispatcher())),
            GetTodayUseCase { LocalDate(2026, 10, 10) },
        )
        viewModel.state.test {
            val before = expectMostRecentItem().offline!!
            assertEquals(0, before.available)
            assertEquals(2, before.total)
            assertFalse(viewModel.state.value.canPublish)
            assertEquals(WeekTiming.CURRENT, viewModel.state.value.setlist?.timing)
            assertFalse(viewModel.state.value.isOffline)
            assertEquals(3, viewModel.state.value.nextRehearsal?.daysLeft, "Saturday 10 Oct. to Tuesday 13 Oct.")

            viewModel.onEvent(WeekEvent.DownloadClicked)
            val after = expectMostRecentItem().offline!!
            assertTrue(after.isComplete)
            assertFalse(after.isDownloading)
        }
    }
}
