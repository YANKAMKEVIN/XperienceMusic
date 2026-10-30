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

    @Test fun recapNamesTheActivityAsPlayed() = runTest {
        val sessions = FakeSessionRepository(member(instrument = Instrument.PIANO))
        val dates = DateProvider { LocalDate(2026, 10, 18) }
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), dates)
        val files = FakeFileStore()
        val repository = FileLearningRepository(SnapshotStore(files))
        val player = FakeAudioPlayer()
        val vm = LearningViewModel(ObserveCurrentMemberUseCase(sessions), repository, growth,
            TickObjectiveUseCase(sessions, growth, GrowthObjectives.all), dates,
            PlayLearningExample(files, player, PlaybackController(player, { it }, backgroundScope)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.onEvent(LearningEvent.Open(0))
        val played = vm.state.value.activity
        vm.onEvent(LearningEvent.Check)
        assertEquals(true, vm.state.value.result)
        assertEquals(played.title, vm.state.value.sessionTitles[played.id])
    }

    private fun TestScope.singerViewModel(script: List<Int?>, instrument: Instrument = Instrument.SINGER): Pair<LearningViewModel, FileLearningRepository> {
        val sessions = FakeSessionRepository(member(instrument = instrument))
        val dates = DateProvider { LocalDate(2026, 10, 19) }
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), dates)
        val files = FakeFileStore()
        val repository = FileLearningRepository(SnapshotStore(files))
        val player = FakeAudioPlayer()
        val playback = PlaybackController(player, { it }, backgroundScope)
        val vm = LearningViewModel(ObserveCurrentMemberUseCase(sessions), repository, growth,
            TickObjectiveUseCase(sessions, growth, GrowthObjectives.all), dates,
            PlayLearningExample(files, player, playback), null, "singer-hold",
            TrackSingingUseCase(FakeMicrophone(script)), PlayReferenceNoteUseCase(files, player, playback))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        return vm to repository
    }

    @Test fun sungActivityPassesOnlyOnceTheMicrophoneHeardTheHold() = runTest {
        // Alto comfort note: Ré4, held long enough for the five-second exercise.
        val (vm, repository) = singerViewModel(List(FakeMicrophone.framesFor(5.5)) { 62 })
        assertTrue(vm.state.value.allowed)
        vm.onEvent(LearningEvent.Open(0))
        vm.onEvent(LearningEvent.Check)
        vm.onEvent(LearningEvent.Next)
        assertEquals(LearningKind.SING, vm.state.value.activity.kind)
        assertEquals(listOf(62), vm.state.value.singTargets)
        assertFalse(vm.state.value.canCheck)
        vm.onEvent(LearningEvent.StartSinging)
        assertTrue(vm.state.value.sung)
        assertFalse(vm.state.value.singing)
        vm.onEvent(LearningEvent.Check)
        assertEquals(true, vm.state.value.result)
        assertTrue(repository.observe("me").first().attempts.last().passed)
    }

    @Test fun offPitchSingingNeverPasses() = runTest {
        val (vm, _) = singerViewModel(List(FakeMicrophone.framesFor(6.0)) { 67 })
        vm.onEvent(LearningEvent.Open(0))
        vm.onEvent(LearningEvent.Check)
        vm.onEvent(LearningEvent.Next)
        vm.onEvent(LearningEvent.StartSinging)
        assertFalse(vm.state.value.sung)
        assertFalse(vm.state.value.canCheck)
    }

    @Test fun singerModulesAreClosedToOtherParts() = runTest {
        val (vm, _) = singerViewModel(emptyList(), Instrument.PIANO)
        assertFalse(vm.state.value.allowed)
    }
}
