package com.xperiencemusic.shared.presentation.player

data class MiniPlayerUiState(
    val isVisible: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    /** 0..1, or 0 while the duration is unknown. */
    val progress: Float = 0f,
    val positionLabel: String = "0:00",
    val durationLabel: String = "",
    /** "2 / 5" while a setlist plays through; null for a single track. */
    val queuePosition: String? = null,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    /** Playing at the slow tempo, to work on a part. */
    val isSlowed: Boolean = false,
    /** The passage played over and over, or its start alone while the end is not marked yet. */
    val loop: LoopUi? = null,
)

/** [endLabel] and [endFraction] are null until B is marked: the loop only runs with both. */
data class LoopUi(
    val startLabel: String,
    val startFraction: Float,
    val endLabel: String? = null,
    val endFraction: Float? = null,
) {
    val isRunning: Boolean get() = endFraction != null
}

sealed interface MiniPlayerEvent {
    data object PlayPauseClicked : MiniPlayerEvent
    data class Seeked(val fraction: Float) : MiniPlayerEvent
    data object CloseClicked : MiniPlayerEvent
    data object RewindClicked : MiniPlayerEvent
    data object ForwardClicked : MiniPlayerEvent
    data object PreviousClicked : MiniPlayerEvent
    data object NextClicked : MiniPlayerEvent
    data object SpeedClicked : MiniPlayerEvent
    /** Marks A, then B, then removes the loop. */
    data object LoopClicked : MiniPlayerEvent
}
