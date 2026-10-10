package com.xperiencemusic.shared.presentation.common

import com.xperiencemusic.shared.domain.model.PlaybackState

/** How one playable item relates to the current playback, for its play/pause button. */
enum class PlaybackStatus { IDLE, LOADING, PLAYING, PAUSED }

fun PlaybackState.statusOf(trackId: String): PlaybackStatus = when {
    track?.id != trackId || hasError -> PlaybackStatus.IDLE
    isLoading -> PlaybackStatus.LOADING
    isPlaying -> PlaybackStatus.PLAYING
    else -> PlaybackStatus.PAUSED
}
