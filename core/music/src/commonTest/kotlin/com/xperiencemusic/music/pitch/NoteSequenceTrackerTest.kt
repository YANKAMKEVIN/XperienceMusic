package com.xperiencemusic.music.pitch

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteSequenceTrackerTest {
    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
    private val frame = 0.05

    private fun NoteSequenceTracker.sing(midi: Int?, seconds: Double): NoteSequenceProgress {
        var last: NoteSequenceProgress? = null
        repeat((seconds / frame).toInt()) { last = push(midi?.let(::hz), frame) }
        return last!!
    }

    @Test
    fun `chaque note tenue fait passer à la suivante jusqu'à la fin`() {
        val tracker = NoteSequenceTracker(listOf(60, 64, 67), secondsEach = 0.5)
        assertEquals(1, tracker.sing(60, 0.6).completedSteps)
        val second = tracker.sing(64, 0.6)
        assertEquals(2, second.completedSteps)
        assertFalse(second.isSuccess)
        assertTrue(tracker.sing(67, 0.6).isSuccess)
    }

    @Test
    fun `chanter la note suivante trop tôt ne compte pas`() {
        val tracker = NoteSequenceTracker(listOf(60, 64), secondsEach = 0.5)
        val early = tracker.sing(64, 1.0)
        assertEquals(0, early.completedSteps)
        assertEquals(60, early.targetMidi)
    }

    @Test
    fun `une seule note longue est une tenue`() {
        val tracker = NoteSequenceTracker(listOf(62), secondsEach = 2.0)
        assertFalse(tracker.sing(62, 1.5).isSuccess)
        assertTrue(tracker.sing(62, 0.6).isSuccess)
    }
}
