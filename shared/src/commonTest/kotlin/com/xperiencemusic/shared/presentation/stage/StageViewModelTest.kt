package com.xperiencemusic.shared.presentation.stage

import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveWeeklySetlistUseCase
import com.xperiencemusic.shared.fakes.setlistRepository
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
        assertEquals("c", middle.nextSongId)
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
    fun `taille du texte et vitesse sont bornées et partagées entre les chants`() = runTest {
        val store = StageSettingsStore()
        val songs = songRepository(a, b)
        val weeklyUseCase = ObserveWeeklySetlistUseCase(setlistRepository(), songs)
        val first = StageViewModel("a", 0, Notation.NASHVILLE, ObserveSongUseCase(songs), weeklyUseCase, store)
        repeat(20) { first.onEvent(StageEvent.TextBiggerClicked) }
        first.onEvent(StageEvent.AutoScrollToggled)
        repeat(20) { first.onEvent(StageEvent.SlowerClicked) }

        val second = StageViewModel("b", 0, Notation.NASHVILLE, ObserveSongUseCase(songs), weeklyUseCase, store)
        assertEquals(StageUiState.MAX_SCALE, store.settings.value.textScale)
        assertEquals(StageUiState.MIN_SPEED, store.settings.value.scrollSpeed)
        assertEquals(true, store.settings.value.isAutoScrolling)
        second.onEvent(StageEvent.TextSmallerClicked)
        assertEquals(StageUiState.MAX_SCALE - StageUiState.SCALE_STEP, store.settings.value.textScale, 0.001f)
    }
}
