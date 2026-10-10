package com.xperiencemusic.shared.domain.model

/** Something that can be listened to: a recording or a voice track. */
data class PlaybackTrack(
    /** Unique among everything playable (recording or voice track id). */
    val id: String,
    val title: String,
    /** Usually the song title. */
    val subtitle: String,
    val audioPath: String,
)

data class PlaybackState(
    val track: PlaybackTrack? = null,
    val isPlaying: Boolean = false,
    /** Resolving the audio link or buffering. */
    val isLoading: Boolean = false,
    val positionMs: Long = 0,
    /** Zero until the player knows it. */
    val durationMs: Long = 0,
    val hasError: Boolean = false,
)
