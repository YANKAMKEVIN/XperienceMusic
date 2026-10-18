package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.domain.learning.PlayLearningExample
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.repository.*
import com.xperiencemusic.shared.domain.usecase.*
import com.xperiencemusic.shared.fakes.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PianoScaleViewModelTest {
    @BeforeTest fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun teardown() = Dispatchers.resetMain()

    private class Microphone: MicrophoneInput {
        val channel = Channel<AudioFrame>(Channel.UNLIMITED)
        var active = false
        override fun frames(frameSize: Int) = flow {
            active = true
            try { for (frame in channel) emit(frame) } finally { active = false }
        }
    }
    private fun TestScope.coach(mic: MicrophoneInput, sessions: FakeSessionRepository = FakeSessionRepository(member(instrument = Instrument.PIANO)), files: FakeFileStore = FakeFileStore()): PianoScaleViewModel {
        val player = FakeAudioPlayer()
        val playback = PlaybackController(player, { it }, backgroundScope)
        return PianoScaleViewModel(ObserveCurrentMemberUseCase(sessions), mic, PlayLearningExample(files, player, playback),
            PlayLocalAudioUseCase(files, player, playback), files, UnconfinedTestDispatcher(testScheduler))
    }
    private suspend fun TestScope.beginRecording(vm: PianoScaleViewModel, mic: Microphone) {
        vm.activate(); vm.start()
        assertEquals(PianoScalePhase.PREPARING, vm.state.value.phase)
        mic.channel.send(AudioFrame(FloatArray(4096) { i -> (sin(2 * PI * 880 * i / 12_000) * 0.3).toFloat() }, 12_000))
        vm.beginCountdown()
        advanceTimeBy(3_000); runCurrent()
        assertEquals(PianoScalePhase.RECORDING, vm.state.value.phase)
    }
    private suspend fun playScale(mic: Microphone, notes: List<Int>) {
        notes.forEach { note -> repeat(3) { frame ->
            val hz = com.xperiencemusic.music.pitch.Pitch.midiToFrequency(note.toDouble())
            mic.channel.send(AudioFrame(FloatArray(4096) { i -> (sin(2 * PI * hz * (frame * 4096 + i) / 12_000) * 0.3).toFloat() }, 12_000))
        } }
    }
    @Test fun permissionFailureDoesNotStartRecording() = runTest {
        val mic = Microphone()
        val vm = coach(mic)
        vm.activate(); vm.denied()
        assertEquals(PianoScalePhase.READY, vm.state.value.phase)
        assertNotNull(vm.state.value.message)
        assertFalse(mic.active)
        vm.interrupt()
    }
    @Test fun backgroundingCancelsMicrophoneAndLatePermissionCannotRestartIt() = runTest {
        val mic = Microphone()
        val vm = coach(mic)
        vm.activate(); vm.start()
        assertTrue(mic.active)
        vm.interrupt()
        advanceUntilIdle()
        assertFalse(mic.active)
        vm.start()
        assertEquals(PianoScalePhase.READY, vm.state.value.phase)
        vm.activate(); vm.start()
        assertTrue(mic.active)
        vm.interrupt()
    }
    @Test fun failedMicrophoneShowsRetryInsteadOfPartialFeedback() = runTest {
        val mic = object: MicrophoneInput { override fun frames(frameSize: Int): Flow<AudioFrame> = flow { throw MicrophoneUnavailableException() } }
        val vm = coach(mic)
        vm.activate(); vm.start()
        assertEquals(PianoScalePhase.READY, vm.state.value.phase)
        assertNotNull(vm.state.value.message)
        assertNull(vm.state.value.report)
        vm.interrupt()
    }
    @Test fun stoppingProducesLocalFeedbackAndExitErasesThePreview() = runTest {
        val mic = Microphone()
        val files = FakeFileStore()
        val vm = coach(mic, files = files)
        beginRecording(vm, mic)
        playScale(mic, listOf(60, 62, 64, 65, 67, 69, 71, 72))
        vm.stop()
        advanceUntilIdle()
        assertFalse(mic.active)
        assertEquals(PianoScalePhase.RESULT, vm.state.value.phase)
        assertEquals(8, vm.state.value.report?.matched)
        vm.listenPassage(3)
        advanceUntilIdle()
        assertNotNull(files.fileUrl("voice/piano-scale-passage.wav"))
        val passageSize = files.files.getValue("voice/piano-scale-passage.wav").size
        vm.listenTake()
        assertTrue(passageSize < files.files.getValue("voice/piano-scale-coach.wav").size)
        assertNotNull(files.fileUrl("voice/piano-scale-coach.wav"))
        vm.interrupt()
        assertNull(files.fileUrl("voice/piano-scale-coach.wav"))
        assertNull(files.fileUrl("voice/piano-scale-passage.wav"))
        assertNull(vm.state.value.report)
    }
    @Test fun signingOutClearsTheTakeAndClosesTheMicrophone() = runTest {
        val mic = Microphone()
        val sessions = FakeSessionRepository(member(instrument = Instrument.PIANO))
        val vm = coach(mic, sessions)
        vm.activate(); vm.start()
        sessions.signOut()
        advanceUntilIdle()
        assertFalse(mic.active)
        assertFalse(vm.state.value.allowed)
        assertNull(vm.state.value.report)
        vm.interrupt()
    }
    @Test fun durationLimitStopsCaptureWithoutAnUnboundedRecording() = runTest {
        val mic = Microphone()
        val vm = coach(mic)
        beginRecording(vm, mic)
        repeat(90) { mic.channel.send(AudioFrame(FloatArray(4096), 12_000)) }
        advanceUntilIdle()
        assertEquals(PianoScalePhase.RESULT, vm.state.value.phase)
        assertEquals(30.0, vm.state.value.seconds)
        assertFalse(mic.active)
        assertEquals(com.xperiencemusic.music.pitch.PianoScaleQuality.TOO_QUIET, vm.state.value.report?.quality)
        vm.interrupt()
    }
    @Test fun signalCheckGatesCountdownAndCanBeCancelledWithoutRecording() = runTest {
        val mic = Microphone(); val vm = coach(mic)
        vm.activate(); vm.start()
        vm.beginCountdown()
        assertEquals(PianoScalePhase.PREPARING, vm.state.value.phase)
        mic.channel.send(AudioFrame(FloatArray(4096) { 1f }, 12_000))
        vm.beginCountdown()
        assertEquals(PianoScalePhase.PREPARING, vm.state.value.phase)
        mic.channel.send(AudioFrame(FloatArray(4096) { i -> (sin(2 * PI * 440 * i / 12_000) * 0.3).toFloat() }, 12_000))
        vm.beginCountdown()
        advanceTimeBy(2_999); runCurrent()
        assertEquals(PianoScalePhase.COUNTDOWN, vm.state.value.phase)
        assertEquals(0.0, vm.state.value.seconds)
        vm.cancelPreparation()
        advanceUntilIdle()
        assertEquals(PianoScalePhase.READY, vm.state.value.phase)
        assertFalse(mic.active)
        assertNull(vm.state.value.report)
        vm.interrupt()
    }
    @Test fun preparationTimesOutAndCanBeRetried() = runTest {
        val mic = Microphone(); val vm = coach(mic)
        vm.activate(); vm.start()
        advanceTimeBy(20_000); runCurrent()
        assertEquals(PianoScalePhase.READY, vm.state.value.phase)
        assertFalse(mic.active)
        assertNotNull(vm.state.value.message)
        vm.start()
        assertTrue(mic.active)
        vm.interrupt()
    }
    @Test fun identicalAttemptsCompareButFocusAndTempoChangesEraseTheBaseline() = runTest {
        val mic = Microphone(); val files = FakeFileStore(); val vm = coach(mic, files = files)
        beginRecording(vm, mic)
        playScale(mic, listOf(60, 62, 64, 66, 67, 69, 71, 72))
        vm.stop(); advanceUntilIdle()
        assertEquals(7, vm.state.value.report?.matched)
        beginRecording(vm, mic)
        playScale(mic, listOf(60, 62, 64, 65, 67, 69, 71, 72))
        vm.stop(); advanceUntilIdle()
        assertEquals(listOf(65), vm.state.value.comparison?.regained)
        vm.listenTake()
        vm.focusStep(3)
        assertEquals(listOf(64, 65, 67), vm.state.value.practice.notes)
        assertNull(vm.state.value.report)
        assertNull(vm.state.value.comparison)
        assertNull(files.fileUrl("voice/piano-scale-coach.wav"))
        beginRecording(vm, mic)
        playScale(mic, listOf(64, 65, 67))
        vm.stop(); advanceUntilIdle()
        assertEquals(3, vm.state.value.report?.matched)
        assertNull(vm.state.value.comparison)
        vm.configure(vm.state.value.practice.copy(bpm = 80))
        assertNull(vm.state.value.report)
        vm.interrupt()
    }
    @Test fun microphoneFailureDuringRecordingDoesNotPublishPartialFeedback() = runTest {
        val mic = Microphone(); val vm = coach(mic)
        beginRecording(vm, mic)
        mic.channel.close(MicrophoneUnavailableException())
        advanceUntilIdle()
        assertEquals(PianoScalePhase.READY, vm.state.value.phase)
        assertNull(vm.state.value.report)
        assertNotNull(vm.state.value.message)
        vm.interrupt()
    }
    @Test fun aNonPianoMemberCannotOpenTheMicrophone() = runTest {
        val mic = Microphone()
        val vm = coach(mic, FakeSessionRepository(member(instrument = Instrument.BASS)))
        vm.activate(); vm.start()
        assertFalse(mic.active)
        assertFalse(vm.state.value.allowed)
        vm.interrupt()
    }
}
