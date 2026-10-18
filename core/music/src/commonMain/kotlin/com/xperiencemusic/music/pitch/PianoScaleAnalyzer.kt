package com.xperiencemusic.music.pitch

import kotlin.math.abs
import kotlin.math.sqrt

/** A deliberately narrow exercise: one ascending C-major octave, one hand, no pedal. */
object PianoScaleExercise {
    val notes = listOf(60, 62, 64, 65, 67, 69, 71, 72)
}

data class HeardPianoNote(val midi: Int, val startSeconds: Double, val endSeconds: Double)
enum class PianoNoteCorrection { MATCH, WRONG_NOTE, WRONG_OCTAVE, MISSING, EXTRA }
data class PianoScaleStep(val expected: Int?, val heard: HeardPianoNote?, val correction: PianoNoteCorrection)
enum class PianoScaleQuality { USABLE, TOO_SHORT, TOO_QUIET, CLIPPED, UNCERTAIN }
data class PianoScaleReport(
    val quality: PianoScaleQuality,
    val heard: List<HeardPianoNote> = emptyList(),
    val steps: List<PianoScaleStep> = emptyList(),
    /** Approximate onset spacing, only assessed for a complete, correctly ordered scale. */
    val irregularSpacing: Boolean? = null,
) {
    val matched get() = steps.count { it.correction == PianoNoteCorrection.MATCH }
}

/**
 * Offline local feedback, not proof of instrumental competence. YIN is monophonic: chords,
 * pedal resonance, harmonics and background tones can still fool it. Silence/noise/clipping
 * produce no verdict. Times are approximate analysis-window positions, not MIDI onsets.
 */
class PianoScaleAnalyzer {
    fun analyze(samples: FloatArray, sampleRate: Int, checkActive: () -> Unit = {}): PianoScaleReport {
        if (sampleRate !in 8_000..192_000 || samples.size.toDouble() / sampleRate < 2.0)
            return PianoScaleReport(PianoScaleQuality.TOO_SHORT)
        if (samples.any { !it.isFinite() }) return PianoScaleReport(PianoScaleQuality.UNCERTAIN)
        if (samples.count { abs(it) >= 0.98f }.toDouble() / samples.size > 0.02)
            return PianoScaleReport(PianoScaleQuality.CLIPPED)
        // Box-filter before integer decimation; preserves the true rate for 44.1 and 48 kHz.
        val factor = (sampleRate / 12_000).coerceAtLeast(1)
        val rate = sampleRate / factor
        val pcm = FloatArray(samples.size / factor) { i ->
            var sum = 0f
            repeat(factor) { sum += samples[i * factor + it] }
            sum / factor
        }
        val detector = YinPitchDetector(rate, minFrequencyHz = 70.0)
        val windowSize = 1024
        val hop = 256
        val stepSeconds = hop.toDouble() / rate
        val heard = mutableListOf<HeardPianoNote>()
        var candidate: Int? = null
        var start = 0.0
        var lastReliable = 0.0
        var reliableDuration = 0.0
        var activeFrames = 0
        var reliableFrames = 0
        fun finish() {
            val midi = candidate
            if (midi != null && reliableDuration >= 0.18) heard += HeardPianoNote(midi, start, lastReliable + stepSeconds)
            candidate = null
            reliableDuration = 0.0
        }
        var offset = 0
        while (offset + windowSize <= pcm.size) {
            checkActive()
            val frame = pcm.copyOfRange(offset, offset + windowSize)
            val time = offset.toDouble() / rate
            val rms = sqrt(frame.sumOf { it.toDouble() * it } / frame.size)
            var note: Int? = null
            if (rms >= 0.008) {
                activeFrames++
                val pitch = detector.detect(frame)
                if (pitch != null && pitch.confidence >= 0.92 && pitch.frequencyHz.isFinite() && pitch.frequencyHz > 0) {
                    val reading = Pitch.nearestNote(pitch.frequencyHz)
                    if (reading.midi in 48..96 && abs(reading.cents) <= 35) note = reading.midi
                }
            }
            if (note != null) {
                reliableFrames++
                if (note != candidate) { finish(); candidate = note; start = time }
                lastReliable = time
                reliableDuration += stepSeconds
            } else if (candidate != null && time - lastReliable > 0.10) finish()
            offset += hop
        }
        finish()
        if (activeFrames * stepSeconds < 0.5) return PianoScaleReport(PianoScaleQuality.TOO_QUIET)
        if (reliableFrames.toDouble() / activeFrames < 0.60 || heard.size < 3 || heard.size > 20)
            return PianoScaleReport(PianoScaleQuality.UNCERTAIN)
        val steps = alignPianoScale(PianoScaleExercise.notes, heard)
        val exact = steps.size == PianoScaleExercise.notes.size && steps.all { it.correction == PianoNoteCorrection.MATCH }
        val intervals = heard.zipWithNext { a, b -> b.startSeconds - a.startSeconds }
        val mean = intervals.average()
        val spacing = if (exact && mean > 0) sqrt(intervals.sumOf { (it - mean) * (it - mean) } / intervals.size) / mean > 0.25 else null
        return PianoScaleReport(PianoScaleQuality.USABLE, heard, steps, spacing)
    }
}

/** Edit-distance alignment keeps later correct notes aligned after a missed or extra note. */
fun alignPianoScale(expected: List<Int>, heard: List<HeardPianoNote>): List<PianoScaleStep> {
    val costs = Array(expected.size + 1) { IntArray(heard.size + 1) }
    for (i in 0..expected.size) costs[i][0] = i
    for (j in 0..heard.size) costs[0][j] = j
    for (i in 1..expected.size) for (j in 1..heard.size) {
        costs[i][j] = minOf(costs[i - 1][j] + 1, costs[i][j - 1] + 1,
            costs[i - 1][j - 1] + (if (expected[i - 1] == heard[j - 1].midi) 0 else 1))
    }
    val result = mutableListOf<PianoScaleStep>()
    var i = expected.size
    var j = heard.size
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && costs[i][j] == costs[i - 1][j - 1] + (if (expected[i - 1] == heard[j - 1].midi) 0 else 1)) {
            val target = expected[--i]
            val note = heard[--j]
            val kind = when {
                note.midi == target -> PianoNoteCorrection.MATCH
                note.midi % 12 == target % 12 -> PianoNoteCorrection.WRONG_OCTAVE
                else -> PianoNoteCorrection.WRONG_NOTE
            }
            result += PianoScaleStep(target, note, kind)
        } else if (i > 0 && costs[i][j] == costs[i - 1][j] + 1) {
            result += PianoScaleStep(expected[--i], null, PianoNoteCorrection.MISSING)
        } else result += PianoScaleStep(null, heard[--j], PianoNoteCorrection.EXTRA)
    }
    return result.asReversed()
}
