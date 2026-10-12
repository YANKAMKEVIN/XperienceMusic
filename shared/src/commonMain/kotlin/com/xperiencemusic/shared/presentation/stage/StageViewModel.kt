package com.xperiencemusic.shared.presentation.stage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Stage display preferences. */
data class StageSettings(
    val textScale: Float = StageUiState.DEFAULT_SCALE,
    val isAutoScrolling: Boolean = false,
    val scrollSpeed: Int = StageUiState.DEFAULT_SPEED,
)

/** App-wide, so text size and scroll speed stay the same from one song to the next. */
class StageSettingsStore {
    val settings = MutableStateFlow(StageSettings())
}

/**
 * Full-screen lyrics for performing. [transposeSemitones] and [notation] come from the song page
 * the musician opened the stage from.
 */
class StageViewModel(
    songId: String,
    private val transposeSemitones: Int,
    private val notation: Notation,
    observeSong: ObserveSongUseCase,
    observeSchedule: ObserveSetlistScheduleUseCase,
    store: StageSettingsStore,
) : ViewModel() {
    private val settings = store.settings

    val state: StateFlow<StageUiState> =
        combine(observeSong(songId), observeSchedule(), settings) { song, schedule, settings ->
            deriveStageState(song, song?.let { schedule.weekOf(it.id) }, settings, transposeSemitones, notation)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StageUiState())

    fun onEvent(event: StageEvent) {
        settings.update {
            when (event) {
                StageEvent.TextBiggerClicked ->
                    it.copy(textScale = (it.textScale + StageUiState.SCALE_STEP).coerceAtMost(StageUiState.MAX_SCALE))
                StageEvent.TextSmallerClicked ->
                    it.copy(textScale = (it.textScale - StageUiState.SCALE_STEP).coerceAtLeast(StageUiState.MIN_SCALE))
                StageEvent.AutoScrollToggled -> it.copy(isAutoScrolling = !it.isAutoScrolling)
                StageEvent.FasterClicked ->
                    it.copy(scrollSpeed = (it.scrollSpeed + StageUiState.SPEED_STEP).coerceAtMost(StageUiState.MAX_SPEED))
                StageEvent.SlowerClicked ->
                    it.copy(scrollSpeed = (it.scrollSpeed - StageUiState.SPEED_STEP).coerceAtLeast(StageUiState.MIN_SPEED))
            }
        }
    }
}

fun deriveStageState(
    song: Song?,
    weekly: WeeklySetlist?,
    settings: StageSettings,
    transposeSemitones: Int,
    notation: Notation,
): StageUiState {
    if (song == null) return StageUiState(isLoading = false)
    val key = song.key.transposedBy(transposeSemitones)
    val keyNotation = if (notation == Notation.NASHVILLE) Notation.SOLFEGE else notation
    val songs = weekly?.songs.orEmpty()
    val order = songs.map { it.id }
    val index = order.indexOf(song.id)
    return StageUiState(
        isLoading = false,
        title = song.title,
        keyLabel = key.label(keyNotation),
        chart = ChordChart.render(song.chart, key, notation),
        textScale = settings.textScale,
        isAutoScrolling = settings.isAutoScrolling,
        scrollSpeed = settings.scrollSpeed,
        setlistPosition = if (index >= 0) "${index + 1} / ${order.size}" else null,
        previousSongId = if (index > 0) order[index - 1] else null,
        nextSongId = if (index >= 0) order.getOrNull(index + 1) else null,
        nextSongTitle = if (index >= 0) songs.getOrNull(index + 1)?.title else null,
    )
}
