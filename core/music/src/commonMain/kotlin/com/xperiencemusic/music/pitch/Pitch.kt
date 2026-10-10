package com.xperiencemusic.music.pitch

import com.xperiencemusic.music.theory.NoteNames
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.music.theory.SEMITONES_PER_OCTAVE
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/** Equal-temperament conversions, A4 = 440 Hz = MIDI 69. */
object Pitch {
    private const val A4_FREQUENCY = 440.0
    private const val A4_MIDI = 69
    private const val CENTS_PER_SEMITONE = 100

    fun frequencyToMidi(frequencyHz: Double): Double =
        A4_MIDI + SEMITONES_PER_OCTAVE * log2(frequencyHz / A4_FREQUENCY)

    fun midiToFrequency(midi: Double): Double =
        A4_FREQUENCY * 2.0.pow((midi - A4_MIDI) / SEMITONES_PER_OCTAVE)

    /** Nearest note to [frequencyHz] and how far off it is, in cents (-50..+50). */
    fun nearestNote(frequencyHz: Double): NoteReading {
        val midi = frequencyToMidi(frequencyHz)
        val nearest = midi.roundToInt()
        return NoteReading(nearest, (midi - nearest) * CENTS_PER_SEMITONE)
    }

    /** Signed distance from [frequencyHz] to [targetMidi], in cents. */
    fun centsFrom(targetMidi: Int, frequencyHz: Double): Double =
        (frequencyToMidi(frequencyHz) - targetMidi) * CENTS_PER_SEMITONE

    /** "La3", "Do#4"… using the scientific octave number (middle C = Do4). */
    fun noteLabel(midi: Int, notation: Notation = Notation.SOLFEGE): String {
        val octave = midi / SEMITONES_PER_OCTAVE - 1
        return NoteNames.name(midi.mod(SEMITONES_PER_OCTAVE), notation, useFlats = false) + octave
    }
}

data class NoteReading(val midi: Int, val cents: Double)
