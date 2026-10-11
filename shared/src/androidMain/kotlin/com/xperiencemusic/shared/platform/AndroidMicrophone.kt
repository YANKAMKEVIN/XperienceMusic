package com.xperiencemusic.shared.platform

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.xperiencemusic.shared.domain.repository.AudioFrame
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import com.xperiencemusic.shared.domain.repository.MicrophoneUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class AndroidMicrophone : MicrophoneInput {

    // The permission is requested by the screen before collecting; a refusal surfaces as an
    // exception from AudioRecord, mapped to MicrophoneUnavailableException.
    @SuppressLint("MissingPermission")
    override fun frames(frameSize: Int): Flow<AudioFrame> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                maxOf(minBuffer, frameSize * Float.SIZE_BYTES * 2),
            )
        } catch (e: SecurityException) {
            throw MicrophoneUnavailableException(e)
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw MicrophoneUnavailableException()
        }
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                val samples = FloatArray(frameSize)
                var filled = 0
                while (filled < frameSize) {
                    val read = record.read(samples, filled, frameSize - filled, AudioRecord.READ_BLOCKING)
                    if (read < 0) throw MicrophoneUnavailableException()
                    filled += read
                }
                emit(AudioFrame(samples, SAMPLE_RATE))
            }
        } finally {
            record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
