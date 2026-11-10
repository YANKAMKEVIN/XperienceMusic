package com.xperiencemusic.shared.presentation.voice

import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.data.repository.InMemoryVoiceSubmissionRepository
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveMySubmissionsUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.SendVoiceUseCase
import com.xperiencemusic.shared.domain.usecase.VoiceRecorder
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeFileStore
import com.xperiencemusic.shared.fakes.FakeMicrophone
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceRecordViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.screen(seconds: Double): VoiceRecordViewModel {
        val today = DateProvider { LocalDate(2026, 11, 8) }
        val sessions = FakeSessionRepository(member())
        val songs = songRepository(song("s1", "À toi la gloire"))
        val submissions = InMemoryVoiceSubmissionRepository(sessions, songs, today)
        val player = FakeAudioPlayer()
        val files = FakeFileStore()
        val playback = PlaybackController(player, { it }, backgroundScope)
        val vm = VoiceRecordViewModel(
            "s1",
            ObserveSongUseCase(songs),
            ObserveCurrentMemberUseCase(sessions),
            ObserveMySubmissionsUseCase(ObserveCurrentMemberUseCase(sessions), submissions),
            VoiceRecorder(FakeMicrophone(List(FakeMicrophone.framesFor(seconds)) { 62 })),
            SendVoiceUseCase(sessions, InMemoryAudioStorage(), submissions, InMemoryExerciseRepository(emptyList()), today),
            playback,
            PlayLocalAudioUseCase(files, player, playback),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test
    fun `quitter l'app pendant l'enregistrement garde la prise et prévient`() = runTest {
        val vm = screen(seconds = 10.0)
        vm.onEvent(VoiceRecordEvent.RecordClicked)
        assertTrue(vm.state.value.hasUnsentTake)

        vm.onEvent(VoiceRecordEvent.AppLeft)

        assertEquals(VoicePhase.RECORDED, vm.state.value.phase)
        assertEquals(VoiceError.INTERRUPTED, vm.state.value.error)
        assertTrue(vm.state.value.hasUnsentTake)
    }

    @Test
    fun `une prise trop courte interrompue revient à l'écran de départ`() = runTest {
        val vm = screen(seconds = 0.5)
        vm.onEvent(VoiceRecordEvent.RecordClicked)

        vm.onEvent(VoiceRecordEvent.AppLeft)

        assertEquals(VoicePhase.READY, vm.state.value.phase)
        assertEquals(VoiceError.INTERRUPTED, vm.state.value.error)
        assertFalse(vm.state.value.hasUnsentTake)
    }

    @Test
    fun `quitter l'app hors enregistrement ne change rien`() = runTest {
        val vm = screen(seconds = 10.0)

        vm.onEvent(VoiceRecordEvent.AppLeft)

        assertEquals(VoicePhase.READY, vm.state.value.phase)
        assertNull(vm.state.value.error)
    }
}
