package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.data.learning.FileLearningRepository
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.usecase.*
import com.xperiencemusic.shared.fakes.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModelTest {
    @BeforeTest fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun teardown() = Dispatchers.resetMain()

    @Test fun memberMustCompleteActivitiesBeforeRequestingEvaluation() = runTest {
        val sessions = FakeSessionRepository(member(instrument = Instrument.PIANO))
        val dates = DateProvider { LocalDate(2026, 10, 18) }
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), dates)
        val files = FakeFileStore()
        val repository = FileLearningRepository(SnapshotStore(files))
        val player = FakeAudioPlayer()
        val playback = PlaybackController(player, { it }, backgroundScope)
        val vm = LearningViewModel(ObserveCurrentMemberUseCase(sessions), repository, growth,
            TickObjectiveUseCase(sessions, growth, GrowthObjectives.all), dates,
            PlayLearningExample(files, player, playback))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.onEvent(LearningEvent.Open(5))
        assertEquals(0, vm.state.value.index)
        vm.onEvent(LearningEvent.RequestReview)
        assertTrue(growth.observeChecks().first().isEmpty())
        vm.onEvent(LearningEvent.Check)
        assertEquals(true, vm.state.value.result)
        vm.onEvent(LearningEvent.Next)
        assertEquals(1, vm.state.value.index)
        vm.onEvent(LearningEvent.Key(60))
        vm.onEvent(LearningEvent.Check)
        assertEquals(false, vm.state.value.result)
        vm.onEvent(LearningEvent.Next)
        assertEquals(1, vm.state.value.index)
        assertTrue(growth.observeChecks().first().isEmpty())
        assertEquals(2, repository.observe("me").first().attempts.size)
    }
}
