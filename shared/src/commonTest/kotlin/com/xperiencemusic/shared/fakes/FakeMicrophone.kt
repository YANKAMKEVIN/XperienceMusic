package com.xperiencemusic.shared.fakes

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.shared.domain.repository.AudioFrame
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import com.xperiencemusic.shared.domain.repository.MicrophoneUnavailableException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.sin

/** Plays a script of notes (MIDI, or null for silence) as synthetic microphone frames. */
class FakeMicrophone(private val script: List<Int?>, private val isUnavailable: Boolean = false) : MicrophoneInput {
    override fun frames(frameSize: Int): Flow<AudioFrame> = flow {
        if (isUnavailable) throw MicrophoneUnavailableException()
        var t = 0
        script.forEach { midi ->
            val samples = FloatArray(frameSize) { i ->
                midi?.let { sin(2 * PI * Pitch.midiToFrequency(it.toDouble()) * (t + i) / SAMPLE_RATE).toFloat() * 0.5f } ?: 0f
            }
            t += frameSize
            emit(AudioFrame(samples, SAMPLE_RATE))
        }
    }

    companion object {
        const val SAMPLE_RATE = 44_100

        /** Frames needed to cover [seconds] with the use case's frame size. */
        fun framesFor(seconds: Double) = (seconds * SAMPLE_RATE / 2048).toInt() + 1
    }
}
