package com.xperiencemusic.shared.presentation.exercise

import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.domain.model.Exercise
import com.xperiencemusic.shared.domain.model.ExerciseCheck
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.SingingProgress
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.usecase.CompleteExerciseUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import com.xperiencemusic.shared.domain.usecase.PlayReferenceNoteUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.TrackSingingUseCase
import com.xperiencemusic.shared.domain.repository.DateProvider
import kotlinx.datetime.LocalDate
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeFileStore
import com.xperiencemusic.shared.fakes.FakeMicrophone
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelTest {
    // Tenor comfort note is Sol3 (55); a fifth above is Ré4 (62).
    private val exercise = Exercise("fifth", 1, 1, "Quinte", "Tiens la quinte.", ExerciseCheck.HoldNote(7, 1.0))
    private val exercises = InMemoryExerciseRepository(listOf(exercise))
    private val sessions = FakeSessionRepository(member(voicePart = VoicePart.TENOR))
    private val player = FakeAudioPlayer()
    private val today = DateProvider { LocalDate(2026, 10, 12) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(mic: FakeMicrophone): ExerciseViewModel {
        val playback = PlaybackController(player, { it }, CoroutineScope(UnconfinedTestDispatcher()))
        return ExerciseViewModel(
            "fifth",
            ObserveCurrentMemberUseCase(sessions),
            ObserveSingerPathUseCase(exercises, today),
            TrackSingingUseCase(mic),
            CompleteExerciseUseCase(sessions, exercises, today),
            PlayReferenceNoteUseCase(FakeFileStore(), player, playback),
            playback,
        )
    }

    @Test
    fun `la note cible dépend de la voix`() = runTest {
        val check = viewModel(FakeMicrophone(emptyList())).state.value.check!!
        assertEquals("Ré4", check.targetNoteLabel)
        assertEquals(62, check.targetMidi)
    }

    @Test
    fun `tenir la note valide l'exercice et l'enregistre`() = runTest {
        val vm = viewModel(FakeMicrophone(List(FakeMicrophone.framesFor(1.0)) { 62 }))
        vm.onEvent(ExerciseEvent.StartClicked)
        assertEquals(ExercisePhase.SUCCEEDED, vm.state.value.phase)
        assertEquals(ExerciseStatus.VALIDATED, vm.state.value.status)
        assertEquals(ExerciseStatus.VALIDATED, exercises.observeStatuses("me").first()["fifth"])
        assertEquals(setOf(LocalDate(2026, 10, 12)), exercises.observePracticeDays("me").first(), "today counts for the streak")
    }

    @Test
    fun `un micro indisponible affiche une erreur`() = runTest {
        val vm = viewModel(FakeMicrophone(emptyList(), isUnavailable = true))
        vm.onEvent(ExerciseEvent.StartClicked)
        assertEquals(ExercisePhase.READY, vm.state.value.phase)
        assertEquals(ExerciseError.MICROPHONE_UNAVAILABLE, vm.state.value.error)
    }

    @Test
    fun `écouter la note de référence joue un son généré`() = runTest {
        viewModel(FakeMicrophone(emptyList())).onEvent(ExerciseEvent.PlayReferenceClicked)
        assertEquals(listOf("file:///app/tones/62-2.wav"), player.played)
    }

    @Test
    fun `l'aiguille et le conseil suivent la justesse`() {
        val check = AutomaticCheckUi("Ré4", 62, 5)
        val low = check.withProgress(SingingProgress(61, -80.0, 0.0, false))
        assertEquals(Tuning.TOO_LOW, low.tuning)
        assertEquals(-0.8f, low.needle!!, 0.001f)
        assertEquals(Tuning.IN_TUNE, check.withProgress(SingingProgress(62, 30.0, 1.0, false)).tuning)
        assertTrue(check.withProgress(SingingProgress(64, 250.0, 0.0, false)).needle == 1f)
    }
}
