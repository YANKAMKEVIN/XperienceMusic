package com.xperiencemusic.music.pitch

/**
 * Monophonic pitch detection with the YIN algorithm (de Cheveigné & Kawahara, 2002).
 * Well suited to a single voice; not meant for chords.
 *
 * @param minFrequencyHz lowest pitch to look for; bounds the largest lag searched.
 * @param threshold absolute threshold on the cumulative mean normalized difference. Lower is stricter.
 */
class YinPitchDetector(
    private val sampleRate: Int,
    minFrequencyHz: Double = 60.0,
    private val threshold: Double = 0.15,
) {
    private val maxLag = (sampleRate / minFrequencyHz).toInt()

    /** Required frame size: two periods of the lowest frequency searched. */
    val minimumFrameSize: Int get() = maxLag * 2

    /** Returns the fundamental of [frame], or null when the frame is silent or unpitched. */
    fun detect(frame: FloatArray): PitchEstimate? {
        require(frame.size >= minimumFrameSize) {
            "frame needs at least $minimumFrameSize samples, got ${frame.size}"
        }
        val window = frame.size / 2
        val difference = DoubleArray(maxLag)
        for (lag in 1 until maxLag) {
            var sum = 0.0
            for (i in 0 until window) {
                val delta = frame[i] - frame[i + lag]
                sum += delta * delta
            }
            difference[lag] = sum
        }

        val normalized = DoubleArray(maxLag)
        normalized[0] = 1.0
        var runningSum = 0.0
        for (lag in 1 until maxLag) {
            runningSum += difference[lag]
            normalized[lag] = if (runningSum == 0.0) 1.0 else difference[lag] * lag / runningSum
        }

        val lag = firstDipBelowThreshold(normalized) ?: return null
        val refinedLag = parabolicInterpolation(normalized, lag)
        return PitchEstimate(
            frequencyHz = sampleRate / refinedLag,
            confidence = (1.0 - normalized[lag]).coerceIn(0.0, 1.0),
        )
    }

    private fun firstDipBelowThreshold(normalized: DoubleArray): Int? {
        var lag = 2
        while (lag < normalized.size) {
            if (normalized[lag] < threshold) {
                while (lag + 1 < normalized.size && normalized[lag + 1] < normalized[lag]) lag++
                return lag
            }
            lag++
        }
        return null
    }

    private fun parabolicInterpolation(values: DoubleArray, lag: Int): Double {
        if (lag <= 0 || lag >= values.size - 1) return lag.toDouble()
        val left = values[lag - 1]
        val center = values[lag]
        val right = values[lag + 1]
        val denominator = left - 2 * center + right
        return if (denominator == 0.0) lag.toDouble() else lag + (left - right) / (2 * denominator)
    }
}

data class PitchEstimate(val frequencyHz: Double, val confidence: Double)
