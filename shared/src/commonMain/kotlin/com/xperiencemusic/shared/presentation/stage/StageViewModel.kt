package com.xperiencemusic.shared.presentation.stage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.music.chart.ChordChart
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.repository.DevicePreferences
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Stage display preferences. */
data class StageSettings(
    val textScale: Float = StageUiState.DEFAULT_SCALE,
    val isAutoScrolling: Boolean = false,
    val scrollSpeed: Int = StageUiState.DEFAULT_SPEED,
)

/**
 * App-wide, so text size and scroll speed stay the same from one song to the next, and kept on the
 * phone: a music stand is set once, not every Sunday. The phone upright and the phone lying on the
 * stand do not show the same amount of text, so each orientation has its own size and speed.
 * Whether the page is scrolling is shared by both and never kept: the stage does not reopen moving.
 */
class StageSettingsStore(private val preferences: DevicePreferences) {
    private val _portrait = MutableStateFlow(read(PORTRAIT))
    private val _landscape = MutableStateFlow(read(LANDSCAPE))
    val portrait: StateFlow<StageSettings> = _portrait.asStateFlow()
    val landscape: StateFlow<StageSettings> = _landscape.asStateFlow()

    fun update(isLandscape: Boolean, change: (StageSettings) -> StageSettings) {
        val next = change(if (isLandscape) _landscape.value else _portrait.value)
        (if (isLandscape) _landscape else _portrait).value = next
        (if (isLandscape) _portrait else _landscape).update { it.copy(isAutoScrolling = next.isAutoScrolling) }
        val orientation = if (isLandscape) LANDSCAPE else PORTRAIT
        preferences.write("$orientation.$SCALE", next.textScale.toString())
        preferences.write("$orientation.$SPEED", next.scrollSpeed.toString())
    }

    /** Anything unreadable or out of range falls back to the defaults. */
    private fun read(orientation: String) = StageSettings(
        textScale = preferences.read("$orientation.$SCALE")?.toFloatOrNull()
            ?.takeIf { it in StageUiState.MIN_SCALE..StageUiState.MAX_SCALE } ?: StageUiState.DEFAULT_SCALE,
        scrollSpeed = preferences.read("$orientation.$SPEED")?.toIntOrNull()
            ?.takeIf { it in StageUiState.MIN_SPEED..StageUiState.MAX_SPEED } ?: StageUiState.DEFAULT_SPEED,
    )

    private companion object {
        const val PORTRAIT = "stage.portrait"
        const val LANDSCAPE = "stage.landscape"
        const val SCALE = "textScale"
        const val SPEED = "scrollSpeed"
    }
}

/**
 * Full-screen lyrics for performing. [transposeSemitones], [notation] and [lyricsOnly] come from
 * the song page the musician opened the stage from.
 */
class StageViewModel(
    songId: String,
    private val transposeSemitones: Int,
    private val notation: Notation,
    private val lyricsOnly: Boolean,
    observeSong: ObserveSongUseCase,
    observeSchedule: ObserveSetlistScheduleUseCase,
    private val store: StageSettingsStore,
) : ViewModel() {
    /** Told by the screen, which alone knows how the phone is held. */
    private val isLandscape = MutableStateFlow(false)
    private val settings = combine(store.portrait, store.landscape, isLandscape) { portrait, landscape, isLandscape ->
        if (isLandscape) landscape else portrait
    }

    val state: StateFlow<StageUiState> =
        combine(observeSong(songId), observeSchedule(), settings) { song, schedule, settings ->
            deriveStageState(song, song?.let { schedule.weekOf(it.id) }, settings, transposeSemitones, notation)
                .copy(lyricsOnly = lyricsOnly)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StageUiState())

    fun onEvent(event: StageEvent) {
        if (event is StageEvent.OrientationChanged) {
            isLandscape.value = event.isLandscape
            return
        }
        store.update(isLandscape.value) {
            when (event) {
                is StageEvent.OrientationChanged -> it
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
        transposeSemitones = transposeSemitones.takeIf { key != song.key } ?: 0,
        originalKeyLabel = song.key.label(keyNotation).takeIf { key != song.key },
        chart = ChordChart.render(song.chart.orEmpty(), key, notation),
        isChartLoading = song.chart == null,
        textScale = settings.textScale,
        isAutoScrolling = settings.isAutoScrolling,
        scrollSpeed = settings.scrollSpeed,
        setlistPosition = if (index >= 0) "${index + 1} / ${order.size}" else null,
        previousSongId = if (index > 0) order[index - 1] else null,
        previousSongTitle = if (index > 0) songs[index - 1].title else null,
        nextSongId = if (index >= 0) order.getOrNull(index + 1) else null,
        nextSongTitle = if (index >= 0) songs.getOrNull(index + 1)?.title else null,
    )
}
