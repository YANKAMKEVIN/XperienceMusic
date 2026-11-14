package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.WavEncoder
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.sqrt

/** One microphone frame, measured. */
class CapturedFrame(val samples: FloatArray, val sampleRate: Int) {
    /** Average loudness (RMS), 0..1. */
    val loudness: Double = PcmRecorder.loudness(samples)

    /** Loudness scaled for a level meter, 0..1. */
    val level: Float get() = (loudness * LEVEL_GAIN).toFloat().coerceIn(0f, 1f)

    /** Share of the samples at full scale: above a few percent, the sound saturates. */
    val clippedShare: Double get() = samples.count { abs(it) >= CLIP_LEVEL }.toDouble() / samples.size

    private companion object {
        const val LEVEL_GAIN = 6.0
        const val CLIP_LEVEL = 0.98f
    }
}

/**
 * Microphone capture kept in memory: reads and measures frames, keeps the ones asked for, then hands
 * the take back as PCM or WAV. Shared by the voice recorder and the piano workshop.
 */
class PcmRecorder(private val microphone: MicrophoneInput) {
    private val chunks = mutableListOf<FloatArray>()

    /** The rate of the frames read so far, 0 before the first one. */
    var sampleRate = 0
        private set

    var sampleCount = 0
        private set

    val frameCount: Int get() = chunks.size

    val seconds: Double get() = if (sampleRate == 0) 0.0 else sampleCount.toDouble() / sampleRate

    /**
     * The microphone's frames. With [strict], a frame that is empty, not finite or at another rate
     * than the previous ones ends the flow with an error instead of corrupting the take.
     */
    fun frames(frameSize: Int = FRAME_SIZE, strict: Boolean = false): Flow<CapturedFrame> =
        microphone.frames(frameSize).map { frame ->
            if (strict) {
                require(frame.sampleRate in MIN_RATE..MAX_RATE && frame.samples.isNotEmpty() && frame.samples.all { it.isFinite() })
                if (sampleRate != 0) require(sampleRate == frame.sampleRate)
            }
            sampleRate = frame.sampleRate
            CapturedFrame(frame.samples, frame.sampleRate)
        }

    /** Keeps a copy of [frame], cut so the take never goes past [maxSamples]. */
    fun keep(frame: CapturedFrame, maxSamples: Int = Int.MAX_VALUE) {
        val remaining = maxSamples - sampleCount
        if (remaining <= 0) return
        val part = frame.samples.copyOf(minOf(frame.samples.size, remaining))
        chunks += part
        sampleCount += part.size
    }

    /** The take so far, in one piece. */
    fun samples(): FloatArray {
        val samples = FloatArray(sampleCount)
        var offset = 0
        chunks.forEach {
            it.copyInto(samples, offset)
            offset += it.size
        }
        return samples
    }

    fun clear() {
        chunks.clear()
        sampleRate = 0
        sampleCount = 0
    }

    companion object {
        const val FRAME_SIZE = 4096
        private const val MIN_RATE = 8_000
        private const val MAX_RATE = 192_000

        /** Enough for a voice or a piano take; halves the file. */
        private const val WAV_SAMPLE_RATE = 24_000

        fun loudness(samples: FloatArray): Double = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)

        fun wav(samples: FloatArray, sampleRate: Int): ByteArray =
            WavEncoder.encode(samples, sampleRate, maxSampleRate = WAV_SAMPLE_RATE)
    }
}
