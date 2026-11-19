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
)

sealed interface MiniPlayerEvent {
    data object PlayPauseClicked : MiniPlayerEvent
    data class Seeked(val fraction: Float) : MiniPlayerEvent
    data object CloseClicked : MiniPlayerEvent
    data object RewindClicked : MiniPlayerEvent
    data object ForwardClicked : MiniPlayerEvent
    data object PreviousClicked : MiniPlayerEvent
    data object NextClicked : MiniPlayerEvent
}
