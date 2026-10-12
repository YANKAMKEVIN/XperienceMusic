package com.xperiencemusic.shared.presentation.stage

import com.xperiencemusic.music.chart.ChartLine

data class StageUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val keyLabel: String = "",
    val chart: List<ChartLine> = emptyList(),
    val textScale: Float = DEFAULT_SCALE,
    val isAutoScrolling: Boolean = false,
    /** Pixels per second, roughly; the screen converts to its own density. */
    val scrollSpeed: Int = DEFAULT_SPEED,
    /** "2 / 5" when the song is in the week's setlist, null otherwise. */
    val setlistPosition: String? = null,
    val previousSongId: String? = null,
    val nextSongId: String? = null,
    val nextSongTitle: String? = null,
) {
    companion object {
        const val DEFAULT_SCALE = 1.6f
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 3f
        const val SCALE_STEP = 0.2f
        const val DEFAULT_SPEED = 30
        const val MIN_SPEED = 10
        const val MAX_SPEED = 120
        const val SPEED_STEP = 10
    }
}

sealed interface StageEvent {
    data object TextBiggerClicked : StageEvent
    data object TextSmallerClicked : StageEvent
    data object AutoScrollToggled : StageEvent
    data object FasterClicked : StageEvent
    data object SlowerClicked : StageEvent
}
