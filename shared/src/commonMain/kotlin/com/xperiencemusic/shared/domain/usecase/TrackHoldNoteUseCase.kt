package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.HoldNoteTracker
import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.music.pitch.YinPitchDetector
import com.xperiencemusic.shared.domain.model.SingingProgress
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.sqrt

/**
 * Listens to the microphone and follows a "hold this note" attempt. Everything is analysed on
 * the phone; no audio is stored.
 */
class TrackHoldNoteUseCase(private val microphone: MicrophoneInput) {

    operator fun invoke(targetMidi: Int, requiredSeconds: Double): Flow<SingingProgress> {
        val tracker = HoldNoteTracker(targetMidi, requiredSeconds)
        var detector: YinPitchDetector? = null
        return microphone.frames(FRAME_SIZE).map { frame ->
            val yin = detector ?: YinPitchDetector(frame.sampleRate).also { detector = it }
            val frequency = if (frame.samples.rms() < SILENCE_RMS) {
                null
            } else {
                yin.detect(frame.samples)?.takeIf { it.confidence >= MIN_CONFIDENCE }?.frequencyHz
            }
            val progress = tracker.push(frequency, FRAME_SIZE.toDouble() / frame.sampleRate)
            SingingProgress(
                heardMidi = frequency?.let { Pitch.nearestNote(it).midi },
                centsFromTarget = frequency?.let { Pitch.centsFrom(targetMidi, it) },
                holdSeconds = progress.currentHoldSeconds,
                isSuccess = progress.isSuccess,
            )
        }
    }

    private fun FloatArray.rms(): Double = sqrt(sumOf { it.toDouble() * it } / size)

    companion object {
        /** ~46 ms at 44.1 kHz: long enough for low voices, short enough for a live needle. */
        const val FRAME_SIZE = 2048
        private const val SILENCE_RMS = 0.01
        private const val MIN_CONFIDENCE = 0.8
    }
}
