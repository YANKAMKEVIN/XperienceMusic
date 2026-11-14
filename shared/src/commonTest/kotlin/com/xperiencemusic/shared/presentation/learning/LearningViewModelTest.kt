package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.PianoBasics
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.GradeResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningKind
import com.xperiencemusic.shared.fakes.FakeMicrophone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModelTest {
    @BeforeTest
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun teardown() = Dispatchers.resetMain()

    private fun TestScope.collect(vm: LearningViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }

    @Test
    fun memberMustCompleteActivitiesBeforeRequestingEvaluation() = runTest {
        val world = LearningWorld(backgroundScope)
        val vm = world.viewModel()
        collect(vm)
        vm.onEvent(LearningEvent.Open(5))
        assertEquals(0, vm.state.value.index)
        vm.onEvent(LearningEvent.RequestReview)
        assertTrue(world.growth.observeChecks().first().isEmpty())
        // A lesson goes on in one tap: "J'ai compris" opens the next step.
        vm.onEvent(LearningEvent.Check)
        assertEquals(1, vm.state.value.index)
        assertNull(vm.state.value.result)
        vm.onEvent(LearningEvent.Key(60))
        vm.onEvent(LearningEvent.Check)
        assertEquals(false, vm.state.value.result)
        assertIs<GradeResult.WrongNotes>(vm.state.value.checkFeedback?.result)
        vm.onEvent(LearningEvent.Next)
        assertEquals(1, vm.state.value.index)
        assertTrue(world.growth.observeChecks().first().isEmpty())
        assertEquals(2, world.repository.attempts.size)
        assertTrue(world.repository.syncs > 0, "every attempt is offered to the server")
    }

    @Test
    fun aFinishedModuleAsksForItsEvaluation() = runTest {
        val world = LearningWorld(backgroundScope)
        PianoBasics.activities.forEach {
            world.repository.record(LearningAttempt(it.id, "me", it.id, PianoBasics.VERSION, true, LocalDate(2026, 10, 18)))
        }
        val vm = world.viewModel()
        collect(vm)
        assertTrue(vm.state.value.complete)
        vm.onEvent(LearningEvent.RequestReview)
        assertEquals(ExerciseStatus.SUBMITTED, vm.state.value.reviewStatus)
        assertNull(vm.state.value.error)
    }

    @Test
    fun aFailedSaveKeepsTheSelectionAndSaysSo() = runTest {
        val world = LearningWorld(backgroundScope)
        val vm = world.viewModel()
        collect(vm)
        vm.onEvent(LearningEvent.Check)
        world.repository.failRecord = true
        vm.onEvent(LearningEvent.Key(60))
        vm.onEvent(LearningEvent.Check)
        assertEquals(LearningError.SAVE_FAILED, vm.state.value.error)
        assertEquals(setOf(60), vm.state.value.notes)
        assertNull(vm.state.value.result)
        assertFalse(vm.state.value.saving)
    }

    @Test
    fun recapNamesTheActivityAsPlayed() = runTest {
        val vm = LearningWorld(backgroundScope).viewModel()
        collect(vm)
        vm.onEvent(LearningEvent.Open(0))
        val played = vm.state.value.activity
        vm.onEvent(LearningEvent.Check)
        assertTrue(played.id in vm.state.value.completed)
        assertEquals(played.title, vm.state.value.sessionTitles[played.id])
    }

    @Test
    fun resumeOnlyShowsOnceAnActivityWasOpened() = runTest {
        val world = LearningWorld(backgroundScope)
        val first = world.viewModel()
        collect(first)
        assertFalse(first.state.value.canResume, "nothing to resume on a first visit")
        first.onEvent(LearningEvent.Open(0))
        first.onEvent(LearningEvent.Home)
        assertTrue(first.state.value.canResume)
        // Kept for the next visit too.
        val again = world.viewModel()
        collect(again)
        assertTrue(again.state.value.canResume)
        assertEquals(0, again.state.value.resumeIndex)
    }

    @Test
    fun theTakesOfTheObjectiveArePlayedFromTheModule() = runTest {
        val world = LearningWorld(backgroundScope)
        world.recordings.send("me", "piano-c2", byteArrayOf(1), 5)
        world.recordings.send("me", "piano-c1", byteArrayOf(2), 6)
        val vm = world.viewModel()
        collect(vm)
        val take = vm.state.value.takes.single()
        assertEquals(1, take.number)
        assertFalse(take.isPlaying)
        vm.onEvent(LearningEvent.PlayTake(take, "Prise d’évaluation", "5 secondes"))
        runCurrent()
        assertTrue(vm.state.value.takes.single().isPlaying)
        assertEquals(take.audioPath, world.player.played.last())
    }

    private fun TestScope.singerViewModel(script: List<Int?>, instrument: Instrument = Instrument.SINGER): Pair<LearningViewModel, LearningWorld> {
        val world = LearningWorld(backgroundScope, instrument, LocalDate(2026, 10, 19), script)
        val vm = world.viewModel("singer-hold")
        collect(vm)
        return vm to world
    }

    @Test
    fun sungActivityPassesOnlyOnceTheMicrophoneHeardTheHold() = runTest {
        // Alto comfort note: Ré4, held long enough for the five-second exercise.
        val (vm, world) = singerViewModel(List(FakeMicrophone.framesFor(5.5)) { 62 })
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
        assertTrue(world.repository.attempts.last().passed)
    }

    @Test
    fun offPitchSingingNeverPasses() = runTest {
        val (vm, _) = singerViewModel(List(FakeMicrophone.framesFor(6.0)) { 67 })
        vm.onEvent(LearningEvent.Open(0))
        vm.onEvent(LearningEvent.Check)
        vm.onEvent(LearningEvent.Next)
        vm.onEvent(LearningEvent.StartSinging)
        assertFalse(vm.state.value.sung)
        assertFalse(vm.state.value.canCheck)
    }

    @Test
    fun aSungActivityCanWaitWithoutPassingAndUnlocksTheNextStep() = runTest {
        val (vm, world) = singerViewModel(emptyList())
        vm.onEvent(LearningEvent.Open(0))
        assertFalse(vm.state.value.canPostpone, "a lesson is read, not postponed")
        vm.onEvent(LearningEvent.Check)
        vm.onEvent(LearningEvent.Next)
        val sung = vm.state.value.activity
        assertEquals(LearningKind.SING, sung.kind)
        assertTrue(vm.state.value.canPostpone)
        vm.onEvent(LearningEvent.Postpone)
        assertEquals(2, vm.state.value.index)
        assertEquals(2, vm.state.value.reached)
        assertTrue(sung.id in vm.state.value.postponed)
        assertTrue(sung.id in vm.state.value.sessionDifficult)
        assertFalse(sung.id in vm.state.value.completed)
        // Nothing is recorded: setting aside is neither a success nor a failure.
        assertTrue(world.repository.attempts.none { it.activityId == sung.id })
        // Kept on the phone for the next visit.
        assertEquals(setOf(sung.id), world.store.session("me", "singer-hold").postponed)
        // The set-aside activity stays open from the steps.
        vm.onEvent(LearningEvent.Open(1))
        assertEquals(1, vm.state.value.index)
    }

    @Test
    fun aRefusedMicrophoneOffersTheSettingsUntilTheNextTake() = runTest {
        val (vm, _) = singerViewModel(List(FakeMicrophone.framesFor(5.5)) { 62 })
        vm.onEvent(LearningEvent.Open(0))
        vm.onEvent(LearningEvent.Check)
        vm.onEvent(LearningEvent.Next)
        vm.onEvent(LearningEvent.MicrophoneDenied)
        assertTrue(vm.state.value.micDenied)
        assertNotNull(vm.state.value.micError)
        vm.onEvent(LearningEvent.StartSinging)
        assertFalse(vm.state.value.micDenied)
        assertNull(vm.state.value.micError)
    }

    @Test
    fun aSilentMicrophoneSaysItHearsNothing() = runTest {
        val (vm, _) = singerViewModel(List(FakeMicrophone.framesFor(3.5)) { null } + List(400) { null })
        vm.onEvent(LearningEvent.Open(0))
        vm.onEvent(LearningEvent.Check)
        // The fake microphone plays its whole script at once; the take is still listening at its end.
        vm.onEvent(LearningEvent.StartSinging)
        assertTrue(vm.state.value.silentSeconds >= 3.0)
        assertEquals(0f, vm.state.value.micLevel)
    }

    @Test
    fun singerModulesAreClosedToOtherParts() = runTest {
        val (vm, _) = singerViewModel(emptyList(), Instrument.PIANO)
        assertFalse(vm.state.value.allowed)
    }

    @Test
    fun theDefaultModuleIsTheFirstPianoOne() {
        assertEquals(LearningCatalog.DEFAULT, LearningCatalog.modulesFor(Instrument.PIANO).first().id)
    }
}
