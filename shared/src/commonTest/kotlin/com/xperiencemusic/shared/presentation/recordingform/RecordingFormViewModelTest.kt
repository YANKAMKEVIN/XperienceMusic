package com.xperiencemusic.shared.presentation.recordingform

import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.domain.model.AudioFile
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.usecase.AddRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.UploadRecordingUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.recording
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
class RecordingFormViewModelTest {
    private val songs = songRepository(
        song("empty"),
        song("full", recordings = listOf(recording("a", 1, true), recording("b", 2), recording("c", 3))),
    )
    private val sessions = FakeSessionRepository(member(Role.CONTENT_ADMIN))
    private val audio = AudioFile("repet.mp3", "audio/mpeg", ByteArray(3 * 1024 * 1024 / 2), durationSeconds = 212)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(songId: String) = RecordingFormViewModel(
        songId,
        ObserveSongUseCase(songs),
        UploadRecordingUseCase(sessions, songs, InMemoryAudioStorage(), AddRecordingUseCase(sessions, songs)),
        GetTodayUseCase { LocalDate(2026, 10, 13) },
    )

    @Test
    fun `la première version est proposée comme référence`() = runTest {
        val state = viewModel("empty").state.value
        assertTrue(state.makeReference)
        assertFalse(state.isFull)
        assertEquals("13 oct. 2026", state.dateLabel)
    }

    @Test
    fun `le fichier choisi est décrit et permet d'envoyer`() = runTest {
        val vm = viewModel("empty")
        vm.onEvent(RecordingFormEvent.FilePicked(audio))
        vm.onEvent(RecordingFormEvent.TitleChanged("Répétition"))
        assertEquals(PickedFileUi("repet.mp3", "1,5 Mo", "3:32"), vm.state.value.file)
        assertTrue(vm.state.value.canSave)
        vm.onEvent(RecordingFormEvent.SaveClicked)
        assertTrue(vm.state.value.isSaved)
        assertEquals(1, songs.song("empty")!!.recordings.size)
    }

    @Test
    fun `un chant complet exige de choisir la version remplacée`() = runTest {
        val vm = viewModel("full")
        vm.onEvent(RecordingFormEvent.FilePicked(audio))
        vm.onEvent(RecordingFormEvent.TitleChanged("Nouvelle"))
        assertTrue(vm.state.value.isFull)
        assertFalse(vm.state.value.canSave)
        vm.onEvent(RecordingFormEvent.ReplaceSelected("b"))
        vm.onEvent(RecordingFormEvent.SaveClicked)
        val titles = songs.song("full")!!.recordings.map { it.title }
        assertEquals(listOf("Version a", "Version c", "Nouvelle"), titles)
    }

    @Test
    fun `annuler le choix garde le fichier précédent`() = runTest {
        val vm = viewModel("empty")
        vm.onEvent(RecordingFormEvent.FilePicked(audio))
        vm.onEvent(RecordingFormEvent.FilePicked(null))
        assertEquals("repet.mp3", vm.state.value.file?.name)
    }
}
