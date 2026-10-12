package com.xperiencemusic.shared.domain.model

/** Live state of a singing attempt: one note held, or several in a row. */
data class SingingProgress(
    /** Nearest note to what the microphone hears, null in silence. */
    val heardMidi: Int?,
    /** Distance to the current target in cents (positive = too high), null when not near any pitch. */
    val centsFromTarget: Double?,
    /** The hold under way on the current note. */
    val holdSeconds: Double,
    val isSuccess: Boolean,
    /** The note being sung, 0-based; always 0 for a single hold. */
    val stepIndex: Int = 0,
    /** Notes already validated. */
    val completedSteps: Int = 0,
)
