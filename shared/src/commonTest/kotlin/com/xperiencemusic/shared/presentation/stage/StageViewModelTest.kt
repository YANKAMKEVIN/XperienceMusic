package com.xperiencemusic.shared.presentation.stage

import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.fakes.song
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import app.cash.turbine.test
import com.xperiencemusic.shared.data.local.FileDevicePreferences
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class StageViewModelTest {
    private val a = song("a", "Alléluia", chart = "[1]Gloire")
    private val b = song("b", "Béni")
    private val c = song("c", "Car tu es saint")
    private val weekly = WeeklySetlist(
        Setlist("w", LocalDate(2026, 10, 13), LocalDate(2026, 10, 18), listOf("a", "b", "c"), null, "x"),
        listOf(a, b, c),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `dans la setlist, on connaît sa place et ses voisins`() {
        val middle = deriveStageState(b, weekly, StageSettings(), 0, Notation.SOLFEGE)
        assertEquals("2 / 3", middle.setlistPosition)
        assertEquals("a", middle.previousSongId)
        assertEquals(a.title, middle.previousSongTitle)
        assertEquals("c", middle.nextSongId)
        assertEquals(c.title, middle.nextSongTitle)
        assertNull(deriveStageState(c, weekly, StageSettings(), 0, Notation.SOLFEGE).nextSongId)
    }

    @Test
    fun `hors setlist, pas de navigation`() {
        val alone = deriveStageState(song("x"), weekly, StageSettings(), 0, Notation.SOLFEGE)
        assertNull(alone.setlistPosition)
        assertNull(alone.previousSongId)
    }

    @Test
    fun `la scène reprend la transposition et la notation de la fiche`() {
        val state = deriveStageState(a, null, StageSettings(), 2, Notation.LETTERS)
        assertEquals("D", state.keyLabel)
        assertEquals("D", ((state.chart.single() as ChartLine.Lyrics).segments.single().chord))
    }

    @Test
    fun `transposée, la scène dit de combien et depuis quelle tonalité`() {
        val transposed = deriveStageState(a, null, StageSettings(), 2, Notation.LETTERS)
        assertEquals(2, transposed.transposeSemitones)
        assertEquals("C", transposed.originalKeyLabel)

        val down = deriveStageState(a, null, StageSettings(), -1, Notation.SOLFEGE)
        assertEquals(-1, down.transposeSemitones)
        assertEquals("Do majeur", down.originalKeyLabel)
    }

    @Test
    fun `dans sa tonalité d'origine, rien n'est signalé`() {
        val original = deriveStageState(a, null, StageSettings(), 0, Notation.SOLFEGE)
        assertEquals(0, original.transposeSemitones)
        assertNull(original.originalKeyLabel)
        // A full octave lands on the same key: nothing to tell the band either.
        assertNull(deriveStageState(a, null, StageSettings(), 12, Notation.SOLFEGE).originalKeyLabel)
    }

    @Test
    fun `taille du texte et vitesse sont bornées et partagées entre les chants`() = runTest {
        val store = StageSettingsStore(FileDevicePreferences(FakeFileStore()))
        val songs = songRepository(a, b)
        val weeklyUseCase = schedule(setlistRepository(), songs)
        val first = StageViewModel("a", 0, Notation.NASHVILLE, false, ObserveSongUseCase(songs), weeklyUseCase, store)
        repeat(20) { first.onEvent(StageEvent.TextBiggerClicked) }
        first.onEvent(StageEvent.AutoScrollToggled)
        repeat(20) { first.onEvent(StageEvent.SlowerClicked) }

        val second = StageViewModel("b", 0, Notation.NASHVILLE, false, ObserveSongUseCase(songs), weeklyUseCase, store)
        assertEquals(StageUiState.MAX_SCALE, store.portrait.value.textScale)
        assertEquals(StageUiState.MIN_SPEED, store.portrait.value.scrollSpeed)
        assertEquals(true, store.portrait.value.isAutoScrolling)
        assertEquals(1, StageUiState(scrollSpeed = store.portrait.value.scrollSpeed).speedLevel)
        assertEquals(12, StageUiState.MAX_LEVEL)
        second.onEvent(StageEvent.TextSmallerClicked)
        assertEquals(StageUiState.MAX_SCALE - StageUiState.SCALE_STEP, store.portrait.value.textScale, 0.001f)
    }

    @Test
    fun `le pupitre est retrouvé au redémarrage, à part pour le portrait et le paysage`() = runTest {
        val files = FakeFileStore()
        val songs = songRepository(a)
        val weeklyUseCase = schedule(setlistRepository(), songs)
        fun stage(store: StageSettingsStore) =
            StageViewModel("a", 0, Notation.NASHVILLE, false, ObserveSongUseCase(songs), weeklyUseCase, store)

        val viewModel = stage(StageSettingsStore(FileDevicePreferences(files)))
        viewModel.onEvent(StageEvent.TextBiggerClicked)
        viewModel.onEvent(StageEvent.OrientationChanged(isLandscape = true))
        repeat(2) { viewModel.onEvent(StageEvent.TextSmallerClicked) }
        viewModel.onEvent(StageEvent.FasterClicked)
        viewModel.onEvent(StageEvent.AutoScrollToggled)

        // The app restarts: both stands come back as they were set, and nothing is scrolling.
        val restarted = StageSettingsStore(FileDevicePreferences(files))
        assertEquals(StageUiState.DEFAULT_SCALE + StageUiState.SCALE_STEP, restarted.portrait.value.textScale, 0.001f)
        assertEquals(StageUiState.DEFAULT_SPEED, restarted.portrait.value.scrollSpeed)
        assertEquals(StageUiState.DEFAULT_SCALE - 2 * StageUiState.SCALE_STEP, restarted.landscape.value.textScale, 0.001f)
        assertEquals(StageUiState.DEFAULT_SPEED + StageUiState.SPEED_STEP, restarted.landscape.value.scrollSpeed)
        assertEquals(false, restarted.landscape.value.isAutoScrolling)

        stage(restarted).state.test {
            assertEquals(StageUiState.DEFAULT_SCALE + StageUiState.SCALE_STEP, expectMostRecentItem().textScale, 0.001f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tourner le téléphone garde le défilement en cours`() = runTest {
        val store = StageSettingsStore(FileDevicePreferences(FakeFileStore()))
        val songs = songRepository(a)
        val viewModel = StageViewModel("a", 0, Notation.NASHVILLE, false, ObserveSongUseCase(songs), schedule(setlistRepository(), songs), store)
        viewModel.onEvent(StageEvent.AutoScrollToggled)
        viewModel.onEvent(StageEvent.OrientationChanged(isLandscape = true))
        assertEquals(true, store.landscape.value.isAutoScrolling)
    }

    @Test
    fun `un réglage illisible revient à la valeur par défaut`() {
        val preferences = FileDevicePreferences(FakeFileStore()).apply {
            write("stage.portrait.textScale", "12")
            write("stage.portrait.scrollSpeed", "vite")
        }
        assertEquals(StageSettings(), StageSettingsStore(preferences).portrait.value)
    }
}
