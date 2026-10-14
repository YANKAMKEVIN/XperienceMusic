package com.xperiencemusic.shared.presentation.songeditor

import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.music.theory.Key
import com.xperiencemusic.music.theory.Mode
import com.xperiencemusic.shared.domain.usecase.DeleteSongUseCase
import com.xperiencemusic.shared.domain.usecase.ImportSongFromLinkUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.SaveSongUseCase
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SongEditorViewModelTest {
    private val songs = songRepository(song(id = "s1", title = "Gloire", chart = "[1]Gloire [5]à toi"))
    private val sessions = FakeSessionRepository(member(Role.CONTENT_ADMIN))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private var page: String? = null

    private fun viewModel(songId: String?) = SongEditorViewModel(
        songId,
        ObserveSongUseCase(songs),
        SaveSongUseCase(sessions, songs),
        DeleteSongUseCase(sessions, songs),
        ImportSongFromLinkUseCase { page },
    )

    @Test
    fun `importer un lien remplit le nouveau chant`() = runTest {
        page = "{t: Chant d'essai}\n{st: Auteur}\n{key: Em}\n{c: © Éditeur}\n{c: Refrain}\n[Em]Gloire"
        val vm = viewModel(null)
        vm.onEvent(SongEditorEvent.ImportLinkChanged("accords.app/JEM999"))
        vm.onEvent(SongEditorEvent.ImportClicked)
        val state = vm.state.value
        assertEquals("Chant d'essai", state.title)
        assertEquals("Auteur", state.artist)
        assertEquals(Key(4, Mode.MINOR), state.key)
        assertEquals("# Refrain\n[Em]Gloire\n# Source\n© Éditeur\naccords.app/JEM999", state.chart)
        assertEquals("", state.importLink)
        assertTrue(state.unknownChords.isEmpty())
    }

    @Test
    fun `un lien vers un autre site est refusé sans rien lire`() = runTest {
        page = "{t: Ne doit pas être lu}"
        val vm = viewModel(null)
        vm.onEvent(SongEditorEvent.ImportLinkChanged("https://exemple.com/JEM414"))
        vm.onEvent(SongEditorEvent.ImportClicked)
        assertEquals(SongImportError.NOT_A_SONG_LINK, vm.state.value.importError)
        assertEquals("", vm.state.value.title)
    }

    @Test
    fun `une page sans chant le dit`() = runTest {
        page = null
        val vm = viewModel(null)
        vm.onEvent(SongEditorEvent.ImportLinkChanged("accords.app/JEM0"))
        vm.onEvent(SongEditorEvent.ImportClicked)
        assertEquals(SongImportError.NOT_FOUND, vm.state.value.importError)
    }

    @Test
    fun `modifier charge le chant et son aperçu en solfège`() = runTest {
        val state = viewModel("s1").state.value
        assertFalse(state.isLoading)
        assertEquals("Gloire", state.title)
        val chords = (state.preview.single() as ChartLine.Lyrics).segments.map { it.chord }
        assertEquals(listOf("Do", "Sol"), chords)
    }

    @Test
    fun `changer de tonalité garde le texte et met à jour l'aperçu`() = runTest {
        val vm = viewModel("s1")
        vm.onEvent(SongEditorEvent.TonicSelected(2))
        assertEquals("[1]Gloire [5]à toi", vm.state.value.chart)
        val chords = (vm.state.value.preview.single() as ChartLine.Lyrics).segments.map { it.chord }
        assertEquals(listOf("Ré", "La"), chords)
    }

    @Test
    fun `les accords non reconnus sont signalés`() = runTest {
        val vm = viewModel(null)
        vm.onEvent(SongEditorEvent.ChartChanged("[Do][Dp][Dp]"))
        assertEquals(listOf("Dp"), vm.state.value.unknownChords)
    }

    @Test
    fun `supprimer demande confirmation puis efface le chant`() = runTest {
        val vm = viewModel("s1")
        vm.onEvent(SongEditorEvent.DeleteClicked)
        assertTrue(vm.state.value.isConfirmingDeletion)
        assertNotNull(songs.song("s1"))
        vm.onEvent(SongEditorEvent.DeleteConfirmed)
        assertTrue(vm.state.value.isDeleted)
        assertNull(songs.song("s1"))
    }

    @Test
    fun `enregistrer un nouveau chant renvoie son id`() = runTest {
        val vm = viewModel(null)
        assertFalse(vm.state.value.canSave)
        vm.onEvent(SongEditorEvent.TitleChanged("Nouveau"))
        vm.onEvent(SongEditorEvent.SaveClicked)
        val id = assertNotNull(vm.state.value.savedSongId)
        assertEquals("Nouveau", songs.song(id)?.title)
    }
}
