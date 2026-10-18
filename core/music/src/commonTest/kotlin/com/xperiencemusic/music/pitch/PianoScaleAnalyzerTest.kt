package com.xperiencemusic.music.pitch

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.*

class PianoScaleAnalyzerTest {
    private fun pcm(notes: List<Int>, rate: Int = 12_000, seconds: Double = 0.6, piano: Boolean = false, durations: List<Double>? = null): FloatArray {
        val values = mutableListOf<Float>()
        notes.forEachIndexed { index, midi ->
            repeat(((durations?.get(index) ?: seconds) * rate).toInt()) { i ->
                val t = i.toDouble() / rate
                val phase = 2 * PI * Pitch.midiToFrequency(midi.toDouble()) * t
                val value = if (piano) (0.65 * sin(phase) + 0.25 * sin(2 * phase) + 0.1 * sin(3 * phase)) * exp(-2.6 * t) else sin(phase)
                values += (value * 0.35).toFloat()
            }
        }
        return values.toFloatArray()
    }
    @Test fun correctScaleIsDetectedAtCommonDeviceSampleRates() {
        listOf(12_000, 44_100, 48_000).forEach { rate ->
            val result = PianoScaleAnalyzer().analyze(pcm(PianoScaleExercise.notes, rate), rate)
            assertEquals(PianoScaleQuality.USABLE, result.quality, "rate=$rate")
            assertEquals(PianoScaleExercise.notes, result.heard.map { it.midi }, "rate=$rate")
            assertEquals(8, result.matched)
            assertEquals(false, result.irregularSpacing)
        }
    }
    @Test fun pianoLikeDecayAndHarmonicsKeepTheFundamental() {
        val result = PianoScaleAnalyzer().analyze(pcm(PianoScaleExercise.notes, piano = true), 12_000)
        assertEquals(PianoScaleQuality.USABLE, result.quality)
        assertEquals(8, result.matched)
    }
    @Test fun wrongNoteAndOctaveAreReportedWithoutShiftingLaterNotes() {
        val notes = PianoScaleExercise.notes.toMutableList().also { it[3] = 66; it[5] = 81 }
        val result = PianoScaleAnalyzer().analyze(pcm(notes), 12_000)
        assertEquals(6, result.matched)
        assertEquals(PianoNoteCorrection.WRONG_NOTE, result.steps[3].correction)
        assertEquals(PianoNoteCorrection.WRONG_OCTAVE, result.steps[5].correction)
        assertEquals(69, result.steps[5].expected)
        assertNull(result.irregularSpacing)
    }
    @Test fun missingAndExtraNotesAlignWithTheRestOfTheScale() {
        val missing = PianoScaleAnalyzer().analyze(pcm(PianoScaleExercise.notes.filter { it != 65 }), 12_000)
        assertEquals(7, missing.matched)
        assertEquals(65, missing.steps.single { it.correction == PianoNoteCorrection.MISSING }.expected)
        val heard = (listOf(59) + PianoScaleExercise.notes).mapIndexed { index, midi -> HeardPianoNote(midi, index.toDouble(), index + 0.7) }
        val extra = alignPianoScale(PianoScaleExercise.notes, heard)
        assertEquals(8, extra.count { it.correction == PianoNoteCorrection.MATCH })
        assertEquals(59, extra.single { it.correction == PianoNoteCorrection.EXTRA }.heard?.midi)
    }
    @Test fun silenceNoiseClippingAndIncompleteSignalDoNotGetANoteVerdict() {
        val analyzer = PianoScaleAnalyzer()
        assertEquals(PianoScaleQuality.TOO_SHORT, analyzer.analyze(FloatArray(100), 12_000).quality)
        assertEquals(PianoScaleQuality.TOO_QUIET, analyzer.analyze(FloatArray(36_000), 12_000).quality)
        val random = Random(7)
        val noise = FloatArray(36_000) { (random.nextFloat() - 0.5f) * 0.2f }
        assertEquals(PianoScaleQuality.UNCERTAIN, analyzer.analyze(noise, 12_000).quality)
        assertEquals(PianoScaleQuality.CLIPPED, analyzer.analyze(FloatArray(36_000) { 1f }, 12_000).quality)
        val single = analyzer.analyze(pcm(listOf(60), seconds = 3.0), 12_000)
        assertEquals(PianoScaleQuality.UNCERTAIN, single.quality)
        assertTrue(single.steps.isEmpty())
    }
    @Test fun aPartialScaleCanSuggestMissingNotesButNeverACompleteSuccess() {
        val result = PianoScaleAnalyzer().analyze(pcm(listOf(60, 62, 64, 65)), 12_000)
        assertEquals(PianoScaleQuality.USABLE, result.quality)
        assertEquals(4, result.matched)
        assertEquals(4, result.steps.count { it.correction == PianoNoteCorrection.MISSING })
        assertNull(result.irregularSpacing)
    }
    @Test fun spacingFeedbackRequiresAllNotesAndDetectsALongHesitation() {
        val result = PianoScaleAnalyzer().analyze(pcm(PianoScaleExercise.notes,
            durations = listOf(0.6, 0.6, 1.8, 0.6, 0.6, 0.6, 0.6, 0.6)), 12_000)
        assertEquals(8, result.matched)
        assertEquals(true, result.irregularSpacing)
    }
    @Test fun analysisAllowsCooperativeCancellation() {
        class Stopped: RuntimeException()
        assertFailsWith<Stopped> { PianoScaleAnalyzer().analyze(pcm(PianoScaleExercise.notes), 12_000) { throw Stopped() } }
    }
}
