package com.xperiencemusic.shared.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Platform audio output. One sound at a time; [play] replaces whatever was playing. */
interface AudioPlayer {
    val state: StateFlow<AudioPlayerState>

    fun play(url: String)

    fun pause()

    fun resume()

    fun seekTo(positionMs: Long)

    fun stop()
}

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val hasEnded: Boolean = false,
    val hasError: Boolean = false,
)
