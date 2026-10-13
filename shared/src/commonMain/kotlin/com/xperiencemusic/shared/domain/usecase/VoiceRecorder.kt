package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.music.pitch.WavEncoder
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.sqrt

/** What the microphone has recorded so far. */
data class RecordingLevel(
    val seconds: Double = 0.0,
    /** Loudness of the last frame, 0..1, to animate the waves. */
    val level: Float = 0f,
    val isRecording: Boolean = false,
    val hasFailed: Boolean = false,
    /** The length limit was reached and the recording stopped by itself. */
    val reachedLimit: Boolean = false,
)

/** A finished recording, ready to be listened to and sent. */
class RecordedVoice(val wav: ByteArray, val durationSeconds: Int)

/**
 * Records the singer's voice in memory, up to [MAX_SECONDS]. Unlike the exercises, the audio is
 * kept: it is what gets sent for review.
 */
class VoiceRecorder(private val microphone: MicrophoneInput) {
    private val _level = MutableStateFlow(RecordingLevel())
    val level: StateFlow<RecordingLevel> = _level.asStateFlow()

    private val chunks = mutableListOf<FloatArray>()
    private var sampleRate = 0
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        chunks.clear()
        sampleRate = 0
        _level.value = RecordingLevel(isRecording = true)
        job = microphone.frames(FRAME_SIZE)
            .onEach { frame ->
                sampleRate = frame.sampleRate
                chunks += frame.samples.copyOf()
                val seconds = chunks.size.toDouble() * FRAME_SIZE / frame.sampleRate
                val loudness = sqrt(frame.samples.sumOf { it.toDouble() * it } / frame.samples.size)
                _level.value = RecordingLevel(seconds, (loudness * LEVEL_GAIN).toFloat().coerceIn(0f, 1f), isRecording = true)
                if (seconds >= MAX_SECONDS) {
                    _level.value = _level.value.copy(isRecording = false, reachedLimit = true)
                    job?.cancel()
                }
            }
            .catch { _level.value = RecordingLevel(hasFailed = true) }
            .launchIn(scope)
    }

    /** Stops and returns the recording, or null when nothing usable was captured. */
    fun stop(): RecordedVoice? {
        job?.cancel()
        _level.value = _level.value.copy(isRecording = false, level = 0f)
        if (chunks.isEmpty() || sampleRate == 0) return null
        val samples = FloatArray(chunks.sumOf { it.size })
        var offset = 0
        chunks.forEach { it.copyInto(samples, offset); offset += it.size }
        return RecordedVoice(
            WavEncoder.encode(samples, sampleRate, maxSampleRate = VOICE_SAMPLE_RATE),
            durationSeconds = (samples.size / sampleRate).coerceAtLeast(1),
        )
    }

    fun discard() {
        job?.cancel()
        chunks.clear()
        _level.value = RecordingLevel()
    }

    companion object {
        const val MAX_SECONDS = 300.0
        private const val FRAME_SIZE = 4096
        /** Enough for a voice; halves the file. */
        private const val VOICE_SAMPLE_RATE = 24_000
        private const val LEVEL_GAIN = 6.0
    }
}
