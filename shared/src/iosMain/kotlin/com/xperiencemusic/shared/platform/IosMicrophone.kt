package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.repository.AudioFrame
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import com.xperiencemusic.shared.domain.repository.MicrophoneUnavailableException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive

/**
 * AVAudioEngine input tap. The tap delivers buffers of whatever size the system picks; they are
 * re-cut into frames of the requested size.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMicrophone : MicrophoneInput {

    override fun frames(frameSize: Int): Flow<AudioFrame> = callbackFlow {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeMeasurement,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null,
        )
        session.setActive(true, error = null)

        val engine = AVAudioEngine()
        val input = engine.inputNode
        val format = input.outputFormatForBus(0u)
        val sampleRate = format.sampleRate.toInt()
        if (sampleRate <= 0) {
            close(MicrophoneUnavailableException())
            return@callbackFlow
        }

        var pending = FloatArray(frameSize)
        var filled = 0
        input.installTapOnBus(0u, bufferSize = frameSize.toUInt(), format = format) { buffer, _ ->
            val channel = buffer?.floatChannelData?.get(0) ?: return@installTapOnBus
            val length = buffer.frameLength.toInt()
            for (i in 0 until length) {
                pending[filled++] = channel[i]
                if (filled == frameSize) {
                    trySend(AudioFrame(pending, sampleRate))
                    pending = FloatArray(frameSize)
                    filled = 0
                }
            }
        }
        engine.prepare()
        if (!engine.startAndReturnError(null)) {
            input.removeTapOnBus(0u)
            close(MicrophoneUnavailableException())
            return@callbackFlow
        }

        awaitClose {
            input.removeTapOnBus(0u)
            engine.stop()
            // Back to plain playback, as the audio player expects.
            session.setCategory(AVAudioSessionCategoryPlayback, mode = AVAudioSessionModeDefault, options = 0u, error = null)
        }
    }
}
