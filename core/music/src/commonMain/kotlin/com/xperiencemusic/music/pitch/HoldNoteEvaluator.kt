package com.xperiencemusic.music.pitch

import kotlin.math.abs

/**
 * Checks a singer's "hold this note" exercise: did they stay within [toleranceCents] of
 * [targetMidi] for at least [requiredSeconds] without interruption?
 *
 * Short dropouts (a breath, a detection miss) up to [maxGapSeconds] do not break the hold.
 */
class HoldNoteEvaluator(
    private val targetMidi: Int,
    private val requiredSeconds: Double,
    private val toleranceCents: Double = 50.0,
    private val maxGapSeconds: Double = 0.2,
) {
    /**
     * @param frames one detected pitch per analysis frame, null when nothing was detected.
     * @param frameSeconds duration covered by each frame.
     */
    fun evaluate(frames: List<Double?>, frameSeconds: Double): HoldNoteResult {
        var longestHold = 0.0
        var currentHold = 0.0
        var gap = 0.0
        val deviations = mutableListOf<Double>()

        frames.forEach { frequency ->
            val cents = frequency?.let { Pitch.centsFrom(targetMidi, it) }
            if (cents != null && abs(cents) <= toleranceCents) {
                // A bridged dropout counts as held time; silence before the hold started does not.
                currentHold += frameSeconds + if (currentHold > 0.0) gap else 0.0
                gap = 0.0
                deviations += cents
            } else {
                gap += frameSeconds
                if (gap > maxGapSeconds) {
                    currentHold = 0.0
                    gap = 0.0
                }
            }
            longestHold = maxOf(longestHold, currentHold)
        }

        return HoldNoteResult(
            longestHoldSeconds = longestHold,
            averageDeviationCents = if (deviations.isEmpty()) null else deviations.average(),
            isSuccess = longestHold >= requiredSeconds,
        )
    }
}

data class HoldNoteResult(
    val longestHoldSeconds: Double,
    /** Positive means sharp, negative flat. Null when the target note was never reached. */
    val averageDeviationCents: Double?,
    val isSuccess: Boolean,
)
