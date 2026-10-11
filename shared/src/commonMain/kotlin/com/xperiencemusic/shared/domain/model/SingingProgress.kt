package com.xperiencemusic.shared.domain.model

/** Live state of a "hold this note" attempt. */
data class SingingProgress(
    /** Nearest note to what the microphone hears, null in silence. */
    val heardMidi: Int?,
    /** Distance to the target in cents (positive = too high), null when not near any pitch. */
    val centsFromTarget: Double?,
    val holdSeconds: Double,
    val isSuccess: Boolean,
)
