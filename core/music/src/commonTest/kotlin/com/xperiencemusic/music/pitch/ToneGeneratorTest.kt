package com.xperiencemusic.music.pitch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToneGeneratorTest {
    private fun dataSize(wav: ByteArray) = wav.size - WavEncoder.HEADER_SIZE

    @Test
    fun `un accord dure le temps demandé`() {
        // 24 kHz, 16-bit mono: two bytes per sample.
        assertEquals(24_000 * 2, dataSize(ToneGenerator.chordWav(listOf(60, 64, 67), seconds = 1.0)))
    }

    @Test
    fun `un arpège joue chaque note à la suite`() {
        assertEquals(3 * 12_000 * 2, dataSize(ToneGenerator.arpeggioWav(listOf(60, 64, 67), secondsEach = 0.5)))
    }

    @Test
    fun `le métronome compte huit temps et se tait entre les clics`() {
        val wav = ToneGenerator.clickWav(bpm = 60)
        assertEquals(8 * 24_000 * 2, dataSize(wav))
        // Half a beat after the first click: silence.
        val middle = WavEncoder.HEADER_SIZE + 12_000 * 2
        assertTrue(wav[middle] == 0.toByte() && wav[middle + 1] == 0.toByte())
    }
}
