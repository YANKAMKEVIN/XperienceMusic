package com.xperiencemusic.music.pitch

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Builds a short WAV file of a pure note, for the singer to hear the target before singing. */
object ToneGenerator {
    private const val SAMPLE_RATE = 44_100
    private const val FADE_SECONDS = 0.05
    private const val AMPLITUDE = 0.5

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
        return header(pcm.size) + pcm
    }

    private fun header(dataSize: Int): ByteArray {
        val bytes = ArrayList<Byte>(44)
        fun text(s: String) = s.forEach { bytes += it.code.toByte() }
        fun int(v: Int) = repeat(4) { bytes += ((v shr (8 * it)) and 0xFF).toByte() }
        fun short(v: Int) = repeat(2) { bytes += ((v shr (8 * it)) and 0xFF).toByte() }
        text("RIFF"); int(36 + dataSize); text("WAVE")
        text("fmt "); int(16); short(1); short(1); int(SAMPLE_RATE); int(SAMPLE_RATE * 2); short(2); short(16)
        text("data"); int(dataSize)
        return bytes.toByteArray()
    }
}
