package com.xperiencemusic.shared.presentation.learning

import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.learning.PianoBasics
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.SubmissionOutcome
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.SubmitObjectiveRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import com.xperiencemusic.shared.domain.usecase.VoiceRecorder
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeFileStore
import com.xperiencemusic.shared.fakes.FakeLearningRepository
import com.xperiencemusic.shared.fakes.FakeMicrophone
import com.xperiencemusic.shared.fakes.FakeObjectiveRecordingRepository
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentViewModelTest {
    @BeforeTest
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun teardown() = Dispatchers.resetMain()

    private val learning = FakeLearningRepository(
        PianoBasics.activities.map { LearningAttempt(it.id, "me", it.id, PianoBasics.VERSION, true, LocalDate(2026, 10, 18)) },
    )
    private val recordings = FakeObjectiveRecordingRepository()

    private fun TestScope.screen(seconds: Double, unavailable: Boolean = false): AssessmentViewModel {
        val sessions = FakeSessionRepository(member(instrument = Instrument.PIANO))
        val growth = InMemoryGrowthRepository(sessions, InMemoryMembersRepository(sessions), { LocalDate(2026, 10, 18) })
        val player = FakeAudioPlayer()
        return AssessmentViewModel(
            LearningCatalog.DEFAULT,
            VoiceRecorder(FakeMicrophone(List(FakeMicrophone.framesFor(seconds)) { 60 }, isUnavailable = unavailable)),
            PlayLocalAudioUseCase(FakeFileStore(), player, PlaybackController(player, { it }, backgroundScope)),
            SubmitObjectiveRecordingUseCase(sessions, learning, recordings, TickObjectiveUseCase(sessions, growth, GrowthObjectives.all)),
        )
    }

    @Test
    fun aTakeIsSentWithItsRequestAndTheScreenSaysSo() = runTest {
        val vm = screen(seconds = 5.0)
        vm.start()
        vm.stop()
        assertTrue(vm.state.value.recordedSeconds >= 3)
        vm.send()
        assertEquals(SubmissionOutcome.REVIEW_REQUESTED, vm.state.value.outcome)
        assertEquals(0, vm.state.value.recordedSeconds)
        assertEquals("piano-c2", recordings.sent.single().objectiveId)
    }

    @Test
    fun aFailedSendingKeepsTheTake() = runTest {
        recordings.failSend = true
        val vm = screen(seconds = 5.0)
        vm.start()
        vm.stop()
        val seconds = vm.state.value.recordedSeconds
        vm.send()
        assertEquals(AssessmentError.SEND_FAILED, vm.state.value.error)
        assertNull(vm.state.value.outcome)
        assertEquals(seconds, vm.state.value.recordedSeconds)
    }

    @Test
    fun aRefusedOrBrokenMicrophoneIsSaid() = runTest {
        val vm = screen(seconds = 5.0, unavailable = true)
        vm.denied()
        assertTrue(vm.state.value.microphoneDenied)
        vm.start()
        assertEquals(AssessmentError.MICROPHONE_UNAVAILABLE, vm.state.value.error)
    }

    @Test
    fun aTooShortTakeIsNotSent() = runTest {
        val vm = screen(seconds = 1.0)
        vm.start()
        vm.stop()
        vm.send()
        assertTrue(recordings.sent.isEmpty())
        assertNull(vm.state.value.outcome)
    }
}
