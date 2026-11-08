package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.NoteSequenceTracker
import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.music.pitch.YinPitchDetector
import com.xperiencemusic.shared.domain.model.SingingProgress
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Listens to the microphone and follows an attempt: one note held, or several in a row. Everything
 * is analysed on the phone; no audio is stored.
 */
class TrackSingingUseCase(private val microphone: MicrophoneInput) {

    operator fun invoke(targetMidis: List<Int>, secondsEach: Double): Flow<SingingProgress> {
        val tracker = NoteSequenceTracker(targetMidis, secondsEach)
        var detector: YinPitchDetector? = null
        var silence = 0.0
        return microphone.frames(FRAME_SIZE).map { frame ->
            val yin = detector ?: YinPitchDetector(frame.sampleRate).also { detector = it }
            val rms = frame.samples.rms()
            val frequency = if (rms < SILENCE_RMS) {
                null
            } else {
                yin.detect(frame.samples)?.takeIf { it.confidence >= MIN_CONFIDENCE }?.frequencyHz
            }
            val seconds = FRAME_SIZE.toDouble() / frame.sampleRate
            silence = if (frequency == null) silence + seconds else 0.0
            val progress = tracker.push(frequency, seconds)
            SingingProgress(
                heardMidi = frequency?.let { Pitch.nearestNote(it).midi },
                centsFromTarget = frequency?.let { Pitch.centsFrom(progress.targetMidi, it) },
                holdSeconds = progress.currentHoldSeconds,
                isSuccess = progress.isSuccess,
                stepIndex = progress.stepIndex,
                completedSteps = progress.completedSteps,
                level = levelOf(rms),
                silentSeconds = silence,
            )
        }
    }

    private fun FloatArray.rms(): Double = sqrt(sumOf { it.toDouble() * it } / size)

    /** -50 dBFS (a quiet room) reads 0, -6 dBFS (a loud voice close to the phone) reads 1. */
    private fun levelOf(rms: Double): Float =
        if (rms <= 0.0) 0f else ((20 * log10(rms) + 50) / 44).toFloat().coerceIn(0f, 1f)

    companion object {
        /** ~46 ms at 44.1 kHz: long enough for low voices, short enough for a live needle. */
        const val FRAME_SIZE = 2048
        private const val SILENCE_RMS = 0.01
        private const val MIN_CONFIDENCE = 0.8
    }
}
