package com.xperiencemusic.shared.domain.repository

/**
 * Small display choices kept on the phone (chord notation, stage text size). They belong to the
 * phone, not the account: signing out keeps them, like the microphone explanation.
 */
interface DevicePreferences {
    fun read(key: String): String?

    fun write(key: String, value: String)
}
