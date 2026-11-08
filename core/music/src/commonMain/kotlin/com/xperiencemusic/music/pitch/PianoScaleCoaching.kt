package com.xperiencemusic.music.pitch

/** Explicit target selection keeps all feedback tied to the exercise that was recorded. */
enum class PianoScaleKey(val ascending: List<Int>) {
    C(listOf(60, 62, 64, 65, 67, 69, 71, 72)),
    G(listOf(67, 69, 71, 72, 74, 76, 78, 79)),
    F(listOf(65, 67, 69, 70, 72, 74, 76, 77)),
}
data class PianoScalePractice(
    val key: PianoScaleKey = PianoScaleKey.C,
    val descending: Boolean = false,
    val bpm: Int = 60,
    val focusStart: Int? = null,
) {
    init { require(bpm in listOf(40, 60, 80)); require(focusStart == null || focusStart in 0..5) }
    val fullNotes get() = if (descending) key.ascending.reversed() else key.ascending
    val notes get() = focusStart?.let { fullNotes.subList(it, it + 3) } ?: fullNotes
    fun focus(expectedIndex: Int): PianoScalePractice {
        require(expectedIndex in notes.indices)
        val center = (focusStart ?: 0) + expectedIndex
        return copy(focusStart = (center - 1).coerceIn(0, fullNotes.size - 3))
    }
}

data class PianoPassage(val startSeconds: Double, val endSeconds: Double)
/** Include context for a missed note; never return an empty or unbounded audio slice. */
fun PianoScaleReport.passage(stepIndex: Int, duration: Double): PianoPassage? {
    if (quality != PianoScaleQuality.USABLE || stepIndex !in steps.indices || duration <= 0) return null
    val note = steps[stepIndex].heard
    val previous = steps.take(stepIndex).lastOrNull { it.heard != null }?.heard
    val next = steps.drop(stepIndex + 1).firstOrNull { it.heard != null }?.heard
    val anchor = note?.startSeconds ?: previous?.endSeconds ?: next?.startSeconds ?: return null
    val start = ((note?.startSeconds ?: previous?.startSeconds ?: anchor) - 0.3).coerceIn(0.0, duration)
    val end = ((note?.endSeconds ?: next?.endSeconds ?: anchor + 1.0) + 0.4)
        .coerceAtMost(duration).coerceAtMost(start + 6.0)
    return if (end > start) PianoPassage(start, end) else null
}

/** A deletion/extra shifts alignment rows; expected-note positions still correspond exactly. */
fun PianoScaleReport.expectedIndexAt(stepIndex: Int): Int? {
    if (stepIndex !in steps.indices || expectedNotes.isEmpty()) return null
    return (steps.take(stepIndex + 1).count { it.expected != null } - 1).coerceIn(expectedNotes.indices)
}
data class PianoScaleComparison(val regained: List<Int>, val toCheckAgain: List<Int>, val previousMatched: Int, val currentMatched: Int)
fun comparePianoScales(previous: PianoScaleReport?, current: PianoScaleReport): PianoScaleComparison? {
    if (previous == null || previous.quality != PianoScaleQuality.USABLE || current.quality != PianoScaleQuality.USABLE || previous.expectedNotes != current.expectedNotes) return null
    val before = previous.steps.filter { it.expected != null }.map { it.correction == PianoNoteCorrection.MATCH }
    val after = current.steps.filter { it.expected != null }.map { it.correction == PianoNoteCorrection.MATCH }
    if (before.size != after.size || after.size != current.expectedNotes.size) return null
    return PianoScaleComparison(after.indices.filter { after[it] && !before[it] }.map { current.expectedNotes[it] },
        after.indices.filter { before[it] && !after[it] }.map { current.expectedNotes[it] }, previous.matched, current.matched)
}
