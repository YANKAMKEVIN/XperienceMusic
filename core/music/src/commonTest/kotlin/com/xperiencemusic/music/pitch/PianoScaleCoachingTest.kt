package com.xperiencemusic.music.pitch

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.*

class PianoScaleCoachingTest {
    private fun report(expected: List<Int>, played: List<Int>) = PianoScaleReport(PianoScaleQuality.USABLE,
        steps = alignPianoScale(expected, played.mapIndexed { index, midi -> HeardPianoNote(midi, index.toDouble(), index + 0.7) }), expectedNotes = expected)
    @Test fun everyKeyAndDirectionHasTheRightTargetAndCanBeAnalyzed() {
        PianoScaleKey.entries.forEach { key -> listOf(false, true).forEach { descending ->
            val practice = PianoScalePractice(key, descending)
            val expected = if (descending) key.ascending.reversed() else key.ascending
            assertEquals(expected, practice.notes)
            val rate = 12_000
            val samples = expected.flatMap { midi -> List(7200) { i ->
                (sin(2 * PI * Pitch.midiToFrequency(midi.toDouble()) * i / rate) * 0.3).toFloat()
            } }.toFloatArray()
            val result = PianoScaleAnalyzer().analyze(samples, rate, expected)
            assertEquals(PianoScaleQuality.USABLE, result.quality)
            assertEquals(8, result.matched)
            assertEquals(expected, result.expectedNotes)
        } }
    }
    @Test fun focusKeepsThreeNotesAtBoundariesAndNeverSwitchesScaleOrDirection() {
        val full = PianoScalePractice(PianoScaleKey.F, descending = true, bpm = 40)
        assertEquals(full.fullNotes.take(3), full.focus(0).notes)
        assertEquals(full.fullNotes.takeLast(3), full.focus(7).notes)
        assertEquals(full.fullNotes.subList(2, 5), full.focus(3).notes)
        assertEquals(40, full.focus(3).bpm)
        assertEquals(3, full.focus(3).focus(0).notes.size)
    }
    @Test fun focusedThreeNoteTargetGetsItsOwnFeedback() {
        val target = PianoScalePractice().focus(3).notes
        val samples = target.flatMap { midi -> List(12_000) { i ->
            (sin(2 * PI * Pitch.midiToFrequency(midi.toDouble()) * i / 12_000) * 0.3).toFloat()
        } }.toFloatArray()
        val result = PianoScaleAnalyzer().analyze(samples, 12_000, target)
        assertEquals(3, result.matched)
        assertEquals(3, result.steps.size)
    }
    @Test fun comparisonAlignsPositionsDespiteMissingAndExtraNotes() {
        val target = PianoScaleExercise.notes
        val previous = report(target, target.filter { it != 65 })
        val now = report(target, listOf(59) + target)
        val comparison = assertNotNull(comparePianoScales(previous, now))
        assertEquals(listOf(65), comparison.regained)
        assertEquals(7, comparison.previousMatched)
        assertEquals(8, comparison.currentMatched)
        assertNull(comparePianoScales(previous, now.copy(quality = PianoScaleQuality.UNCERTAIN)))
        assertNull(comparePianoScales(previous, report(target.take(3), target.take(3))))
    }
    @Test fun passagesAreBoundedAndMissingNotesIncludeContext() {
        val target = PianoScaleExercise.notes
        val r = report(target, target.filter { it != 65 })
        val missing = r.steps.indexOfFirst { it.correction == PianoNoteCorrection.MISSING }
        val passage = assertNotNull(r.passage(missing, 7.0))
        assertTrue(passage.startSeconds >= 0)
        assertTrue(passage.endSeconds <= 7.0)
        assertTrue(passage.endSeconds > passage.startSeconds)
        assertTrue(passage.endSeconds - passage.startSeconds <= 6.0)
        assertNull(r.passage(-1, 7.0))
        assertNull(r.passage(0, 0.0))
        assertNull(r.copy(quality = PianoScaleQuality.UNCERTAIN).passage(0, 7.0))
        assertEquals(3, r.expectedIndexAt(missing))
    }
}
