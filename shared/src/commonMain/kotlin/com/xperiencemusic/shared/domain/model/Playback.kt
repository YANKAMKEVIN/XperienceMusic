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
    /** Where [track] stands in the list being played through (a setlist); 0 for a single track. */
    val queueIndex: Int = 0,
    /** How many tracks play one after the other; 1 for a single track, 0 when nothing is loaded. */
    val queueSize: Int = 0,
    /** 1 at the recording's tempo, [SLOW_SPEED] to work on a part. */
    val speed: Float = NORMAL_SPEED,
    /** Start of the passage played over and over; null when no loop is being set. */
    val loopStartMs: Long? = null,
    /** End of that passage; null until it is marked, the loop only runs once both are. */
    val loopEndMs: Long? = null,
) {
    val isSlowed: Boolean get() = speed < NORMAL_SPEED

    val hasPrevious: Boolean get() = queueIndex > 0
    val hasNext: Boolean get() = queueIndex < queueSize - 1

    companion object {
        const val NORMAL_SPEED = 1f
        /** Slow enough to hear each note of a harmony, fast enough to still sound like the song. */
        const val SLOW_SPEED = 0.75f
    }
}
