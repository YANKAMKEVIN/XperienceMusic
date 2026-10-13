package com.xperiencemusic.music.pitch

import kotlin.test.Test
import kotlin.test.assertEquals

class WavEncoderTest {
    private fun ByteArray.int(at: Int) = (0..3).sumOf { (this[at + it].toInt() and 0xFF) shl (8 * it) }

    @Test
    fun `une voix enregistrée à 44 kHz est réduite de moitié`() {
        val wav = WavEncoder.encode(FloatArray(44_100) { 0.5f }, sampleRate = 44_100, maxSampleRate = 24_000)
        assertEquals(22_050, wav.int(24), "sample rate")
        assertEquals(22_050 * 2, wav.int(40), "data size")
        assertEquals(WavEncoder.HEADER_SIZE + 22_050 * 2, wav.size)
    }

    @Test
    fun `sans limite le débit d'origine est gardé`() {
        val wav = WavEncoder.encode(FloatArray(100), sampleRate = 48_000)
        assertEquals(48_000, wav.int(24))
    }
}
