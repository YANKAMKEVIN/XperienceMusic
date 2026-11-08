package com.xperiencemusic.shared.domain.repository

/**
 * Whether the app already explained the microphone before the phone's own prompt. Kept for the
 * phone, not the member: the permission belongs to the phone too.
 */
interface MicrophoneIntroStore {
    fun wasShown(): Boolean

    fun markShown()
}
