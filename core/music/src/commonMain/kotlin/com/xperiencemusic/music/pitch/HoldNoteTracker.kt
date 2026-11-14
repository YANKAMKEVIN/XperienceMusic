package com.xperiencemusic.music.pitch

import kotlin.math.abs

/**
 * Checks a singer's "hold this note" exercise one analysis frame at a time while they sing, so a
 * screen can show progress live: did they stay within [toleranceCents] of [targetMidi] for at least
 * [requiredSeconds] without interruption? Short dropouts (a breath, a detection miss) up to
 * [maxGapSeconds] do not break the hold.
 */
class HoldNoteTracker(
    private val targetMidi: Int,
    private val requiredSeconds: Double,
    private val toleranceCents: Double = 50.0,
    private val maxGapSeconds: Double = 0.2,
) {
    private var longestHold = 0.0
    private var currentHold = 0.0
    private var gap = 0.0

    /** Adds one frame ([frequency] null when nothing was detected) and returns the live state. */
    fun push(frequency: Double?, frameSeconds: Double): HoldNoteProgress {
        val cents = frequency?.let { Pitch.centsFrom(targetMidi, it) }
        if (cents != null && abs(cents) <= toleranceCents) {
            // A bridged dropout counts as held time; silence before the hold started does not.
            currentHold += frameSeconds + if (currentHold > 0.0) gap else 0.0
            gap = 0.0
        } else {
            gap += frameSeconds
            if (gap > maxGapSeconds) {
                currentHold = 0.0
                gap = 0.0
            }
        }
        longestHold = maxOf(longestHold, currentHold)
        return HoldNoteProgress(
            currentHoldSeconds = currentHold,
            centsFromTarget = cents,
            isSuccess = longestHold >= requiredSeconds,
        )
    }
}

data class HoldNoteProgress(
    /** The hold under way; back to 0 when the voice drifts away for too long. */
    val currentHoldSeconds: Double,
    /** Distance of this frame's pitch to the target, null when no pitch was heard. */
    val centsFromTarget: Double?,
    /** True once the required duration has been held at least once. */
    val isSuccess: Boolean,
)
