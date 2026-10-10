package com.xperiencemusic.shared.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform audio output. One sound at a time; [play] replaces whatever was playing. Playback goes
 * on with the screen off, with system controls (lock screen, notification) showing [NowPlaying].
 */
interface AudioPlayer {
    val state: StateFlow<AudioPlayerState>

    fun play(url: String, nowPlaying: NowPlaying)

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

/** What the system media controls display. */
data class NowPlaying(val title: String, val subtitle: String)
