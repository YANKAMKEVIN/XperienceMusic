package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.MicrophoneIntroStore

/** A marker file outside the member's data: signing out does not bring the explanation back. */
class FileMicrophoneIntroStore(private val files: FileStore) : MicrophoneIntroStore {
    override fun wasShown(): Boolean = files.readText(PATH) != null

    override fun markShown() = files.writeText(PATH, "shown")

    private companion object {
        const val PATH = "device/microphone-intro.txt"
    }
}
