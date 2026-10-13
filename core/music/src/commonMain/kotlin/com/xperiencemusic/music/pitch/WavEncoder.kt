package com.xperiencemusic.music.pitch

import kotlin.math.roundToInt

/** 16-bit mono PCM WAV files: generated tones and the singers' recordings. */
object WavEncoder {
    /**
     * Encodes [samples] (-1..1) recorded at [sampleRate]. With [maxSampleRate], the audio is
     * averaged down by a whole factor first, which halves a voice recording's size at no audible cost.
     */
    fun encode(samples: FloatArray, sampleRate: Int, maxSampleRate: Int = sampleRate): ByteArray {
        val factor = maxOf(1, (sampleRate + maxSampleRate - 1) / maxSampleRate)
        val count = samples.size / factor
        val pcm = ByteArray(count * 2)
        for (i in 0 until count) {
            var sum = 0f
            for (j in 0 until factor) sum += samples[i * factor + j]
            val value = ((sum / factor).coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt()
            pcm[2 * i] = (value and 0xFF).toByte()
            pcm[2 * i + 1] = ((value shr 8) and 0xFF).toByte()
        }
        return header(pcm.size, sampleRate / factor) + pcm
    }

    internal fun header(dataSize: Int, sampleRate: Int): ByteArray {
        val bytes = ArrayList<Byte>(HEADER_SIZE)
        fun text(s: String) = s.forEach { bytes += it.code.toByte() }
        fun int(v: Int) = repeat(4) { bytes += ((v shr (8 * it)) and 0xFF).toByte() }
        fun short(v: Int) = repeat(2) { bytes += ((v shr (8 * it)) and 0xFF).toByte() }
        text("RIFF"); int(36 + dataSize); text("WAVE")
        text("fmt "); int(16); short(1); short(1); int(sampleRate); int(sampleRate * 2); short(2); short(16)
        text("data"); int(dataSize)
        return bytes.toByteArray()
    }

    const val HEADER_SIZE = 44
}
