package com.xperiencemusic.music.pitch

/**
 * Follows a singer through several notes in a row (an interval, a scale): each note must be held
 * [secondsEach] before the next one counts. One note with a long hold is a plain "hold this note".
 */
class NoteSequenceTracker(
    private val targetMidis: List<Int>,
    private val secondsEach: Double,
    private val toleranceCents: Double = 50.0,
    private val maxGapSeconds: Double = 0.2,
) {
    init {
        require(targetMidis.isNotEmpty()) { "A sequence needs at least one note" }
    }

    private var step = 0
    private var tracker = trackerFor(0)

    /** Adds one frame ([frequency] null when nothing was detected) and returns the live state. */
    fun push(frequency: Double?, frameSeconds: Double): NoteSequenceProgress {
        if (step == targetMidis.size) return finished()
        val progress = tracker.push(frequency, frameSeconds)
        val current = step
        if (progress.isSuccess) {
            step++
            if (step < targetMidis.size) tracker = trackerFor(step)
        }
        return NoteSequenceProgress(
            stepIndex = current,
            stepCount = targetMidis.size,
            targetMidi = targetMidis[current],
            currentHoldSeconds = progress.currentHoldSeconds,
            centsFromTarget = progress.centsFromTarget,
            completedSteps = step,
            isSuccess = step == targetMidis.size,
        )
    }

    private fun finished() = NoteSequenceProgress(
        stepIndex = targetMidis.lastIndex,
        stepCount = targetMidis.size,
        targetMidi = targetMidis.last(),
        currentHoldSeconds = secondsEach,
        centsFromTarget = null,
        completedSteps = targetMidis.size,
        isSuccess = true,
    )

    private fun trackerFor(index: Int) = HoldNoteTracker(targetMidis[index], secondsEach, toleranceCents, maxGapSeconds)
}

data class NoteSequenceProgress(
    /** The note being sung, 0-based. */
    val stepIndex: Int,
    val stepCount: Int,
    val targetMidi: Int,
    /** The hold under way on the current note. */
    val currentHoldSeconds: Double,
    /** Distance of this frame's pitch to the current note, null when no pitch was heard. */
    val centsFromTarget: Double?,
    /** Notes already validated, the one just finished included. */
    val completedSteps: Int,
    val isSuccess: Boolean,
)
