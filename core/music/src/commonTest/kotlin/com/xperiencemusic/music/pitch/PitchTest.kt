package com.xperiencemusic.music.pitch

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PitchTest {
    private val sampleRate = 44_100

    private fun sine(frequency: Double, samples: Int): FloatArray =
        FloatArray(samples) { i -> sin(2 * PI * frequency * i / sampleRate).toFloat() * 0.6f }

    @Test
    fun `nearestNote trouve la note et l'écart en cents`() {
        val reading = Pitch.nearestNote(445.0)
        assertEquals(69, reading.midi)
        assertTrue(reading.cents in 19.0..20.0, "cents=${reading.cents}")
    }

    @Test
    fun `noteLabel utilise le numéro d'octave scientifique`() {
        assertEquals("La4", Pitch.noteLabel(69))
        assertEquals("Do4", Pitch.noteLabel(60))
    }

    @Test
    fun `yin détecte la fréquence d'une sinusoïde`() {
        val detector = YinPitchDetector(sampleRate)
        listOf(110.0, 220.0, 329.63, 523.25).forEach { frequency ->
            val estimate = assertNotNull(detector.detect(sine(frequency, detector.minimumFrameSize)))
            val cents = abs(Pitch.frequencyToMidi(estimate.frequencyHz) - Pitch.frequencyToMidi(frequency)) * 100
            assertTrue(cents < 5, "expected $frequency Hz, got ${estimate.frequencyHz} Hz")
        }
    }

    @Test
    fun `la note de référence générée est reconnue par le détecteur`() {
        val wav = ToneGenerator.wav(midi = 57, seconds = 0.5) // La3
        assertEquals("RIFF", wav.copyOfRange(0, 4).decodeToString())
        val samples = FloatArray((wav.size - 44) / 2) { i ->
            val low = wav[44 + 2 * i].toInt() and 0xFF
            val high = wav[45 + 2 * i].toInt()
            ((high shl 8) or low).toShort() / Short.MAX_VALUE.toFloat()
        }
        val detector = YinPitchDetector(sampleRate)
        val frame = samples.copyOfRange(5_000, 5_000 + detector.minimumFrameSize)
        assertEquals(57, Pitch.nearestNote(assertNotNull(detector.detect(frame)).frequencyHz).midi)
    }

    @Test
    fun `yin ne renvoie rien sur du silence`() {
        val detector = YinPitchDetector(sampleRate)
        assertNull(detector.detect(FloatArray(detector.minimumFrameSize)))
    }

    @Test
    fun `holdNote valide une note tenue assez longtemps malgré une courte coupure`() {
        val la = Pitch.midiToFrequency(69.0)
        val frames = List(20) { la } + listOf(null) + List(15) { la * 1.01 } // +17 cents
        val result = HoldNoteEvaluator(targetMidi = 69, requiredSeconds = 3.0).evaluate(frames, frameSeconds = 0.1)
        assertTrue(result.isSuccess)
        assertEquals(3.6, result.longestHoldSeconds, absoluteTolerance = 1e-9)
    }

    @Test
    fun `le suivi en direct remonte l'écart et la tenue en cours`() {
        val tracker = HoldNoteTracker(targetMidi = 69, requiredSeconds = 0.3)
        val sharp = tracker.push(Pitch.midiToFrequency(69.2), 0.1)
        assertEquals(20.0, sharp.centsFromTarget!!, absoluteTolerance = 0.01)
        assertEquals(0.1, sharp.currentHoldSeconds, absoluteTolerance = 1e-9)
        assertFalse(sharp.isSuccess)
        tracker.push(Pitch.midiToFrequency(69.0), 0.1)
        assertTrue(tracker.push(Pitch.midiToFrequency(68.9), 0.1).isSuccess)
        assertNull(tracker.push(null, 0.1).centsFromTarget)
    }

    @Test
    fun `holdNote échoue si la voix sort de la tolérance`() {
        val la = Pitch.midiToFrequency(69.0)
        val laDiese = Pitch.midiToFrequency(70.0)
        val frames = List(15) { la } + List(5) { laDiese } + List(15) { la }
        val result = HoldNoteEvaluator(targetMidi = 69, requiredSeconds = 2.0).evaluate(frames, frameSeconds = 0.1)
        assertFalse(result.isSuccess)
        assertEquals(1.5, result.longestHoldSeconds, absoluteTolerance = 1e-9)
    }
}
