package com.xperiencemusic.shared.domain.repository

import kotlinx.coroutines.flow.Flow

/** Live microphone audio. Listening starts when the flow is collected and stops when it ends. */
interface MicrophoneInput {
    /**
     * Mono frames of exactly [frameSize] samples in -1..1. Fails with
     * [MicrophoneUnavailableException] when the microphone cannot be opened (permission refused,
     * used by another app).
     */
    fun frames(frameSize: Int): Flow<AudioFrame>
}

class AudioFrame(val samples: FloatArray, val sampleRate: Int)

class MicrophoneUnavailableException(cause: Throwable? = null) : Exception("Microphone unavailable", cause)
