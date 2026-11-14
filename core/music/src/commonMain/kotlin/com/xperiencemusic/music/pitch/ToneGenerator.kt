package com.xperiencemusic.music.pitch

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Synthesized WAV files: a pure note for the singer to hear the target before singing, and the
 * chords, arpeggios and metronome clicks of the guided lessons.
 */
object ToneGenerator {
    private const val SAMPLE_RATE = 44_100
    private const val FADE_SECONDS = 0.05
    private const val AMPLITUDE = 0.5

    /** The lessons' examples: a lighter rate, short fades and a softer level, shared by every note. */
    private const val EXAMPLE_SAMPLE_RATE = 24_000
    private const val EXAMPLE_FADE_SECONDS = 0.015
    private const val EXAMPLE_AMPLITUDE = 0.32

    private const val CLICK_HZ = 1_100.0
    private const val CLICK_SECONDS = 0.035
    private const val CLICK_AMPLITUDE = 0.2

    /** 16-bit mono PCM WAV of [midi] lasting [seconds], with short fades to avoid clicks. */
    fun wav(midi: Int, seconds: Double = 2.0): ByteArray {
        val frequency = Pitch.midiToFrequency(midi.toDouble())
        val count = (SAMPLE_RATE * seconds).toInt()
        val fade = (SAMPLE_RATE * FADE_SECONDS).toInt()
        val pcm = ByteArray(count * 2)
        for (i in 0 until count) {
            val envelope = min(1.0, min(i, count - 1 - i).toDouble() / fade)
            val value = (sin(2 * PI * frequency * i / SAMPLE_RATE) * AMPLITUDE * envelope * Short.MAX_VALUE).roundToInt()
            pcm[2 * i] = (value and 0xFF).toByte()
            pcm[2 * i + 1] = ((value shr 8) and 0xFF).toByte()
        }
        return WavEncoder.header(pcm.size, SAMPLE_RATE) + pcm
    }

    /** [notes] sounding together for [seconds]. */
    fun chordWav(notes: List<Int>, seconds: Double): ByteArray = exampleWav(notes, seconds, arpeggio = false)

    /** [notes] one after the other, [secondsEach] each. */
    fun arpeggioWav(notes: List<Int>, secondsEach: Double): ByteArray = exampleWav(notes, secondsEach, arpeggio = true)

    /** [beats] metronome clicks at [bpm]. */
    fun clickWav(bpm: Int, beats: Int = 8): ByteArray {
        val beat = 60.0 / bpm
        val samples = FloatArray((EXAMPLE_SAMPLE_RATE * beat * beats).toInt()) { i ->
            val t = (i.toDouble() / EXAMPLE_SAMPLE_RATE) % beat
            if (t < CLICK_SECONDS) {
                (sin(2 * PI * CLICK_HZ * t) * CLICK_AMPLITUDE * (1 - t / CLICK_SECONDS)).toFloat()
            } else {
                0f
            }
        }
        return WavEncoder.encode(samples, EXAMPLE_SAMPLE_RATE)
    }

    private fun exampleWav(notes: List<Int>, seconds: Double, arpeggio: Boolean): ByteArray {
        require(notes.isNotEmpty())
        val duration = if (arpeggio) seconds * notes.size else seconds
        val samples = FloatArray((EXAMPLE_SAMPLE_RATE * duration).toInt()) { i ->
            val t = i.toDouble() / EXAMPLE_SAMPLE_RATE
            val active = if (arpeggio) listOf(notes[(t / seconds).toInt().coerceAtMost(notes.lastIndex)]) else notes
            val local = if (arpeggio) t % seconds else t
            val envelope = min(1.0, min(local, seconds - local) / EXAMPLE_FADE_SECONDS).coerceAtLeast(0.0)
            val sum = active.sumOf { midi -> sin(2 * PI * Pitch.midiToFrequency(midi.toDouble()) * local) }
            (sum / active.size * EXAMPLE_AMPLITUDE * envelope).toFloat()
        }
        return WavEncoder.encode(samples, EXAMPLE_SAMPLE_RATE)
    }
}
