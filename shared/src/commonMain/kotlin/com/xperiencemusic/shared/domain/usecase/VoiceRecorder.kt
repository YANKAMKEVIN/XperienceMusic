package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

/**
 * A finished recording, ready to be listened to and sent. [isNearlySilent] when the microphone
 * heard almost nothing: worth a warning before it goes to the responsable.
 */
class RecordedVoice(val wav: ByteArray, val durationSeconds: Int, val isNearlySilent: Boolean = false)

/**
 * Records the singer's voice in memory, up to [MAX_SECONDS]. Unlike the exercises, the audio is
 * kept: it is what gets sent for review.
 */
class VoiceRecorder(microphone: MicrophoneInput) {
    private val _level = MutableStateFlow(RecordingLevel())
    val level: StateFlow<RecordingLevel> = _level.asStateFlow()

    private val recorder = PcmRecorder(microphone)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        recorder.clear()
        _level.value = RecordingLevel(isRecording = true)
        job = recorder.frames(FRAME_SIZE)
            .onEach { frame ->
                recorder.keep(frame)
                val seconds = recorder.frameCount.toDouble() * FRAME_SIZE / frame.sampleRate
                _level.value = RecordingLevel(seconds, frame.level, isRecording = true)
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
        val sampleRate = recorder.sampleRate
        if (recorder.frameCount == 0 || sampleRate == 0) return null
        val samples = recorder.samples()
        return RecordedVoice(
            PcmRecorder.wav(samples, sampleRate),
            durationSeconds = (samples.size / sampleRate).coerceAtLeast(1),
            isNearlySilent = PcmRecorder.loudness(samples) < SILENCE_LOUDNESS,
        )
    }

    fun discard() {
        job?.cancel()
        recorder.clear()
        _level.value = RecordingLevel()
    }

    companion object {
        const val MAX_SECONDS = 300.0
        private const val FRAME_SIZE = PcmRecorder.FRAME_SIZE

        /** Average loudness (RMS) under which a take is mostly silence: about -46 dBFS. */
        internal const val SILENCE_LOUDNESS = 0.005
    }
}
