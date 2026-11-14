package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.MicrophoneIntroStore

/** Whether the app already explained the microphone before the phone's own prompt. */
class WasMicrophoneExplainedUseCase(private val store: MicrophoneIntroStore) {
    operator fun invoke(): Boolean = store.wasShown()
}

/** The explanation was read: the next requests go straight to the phone's prompt. */
class MarkMicrophoneExplainedUseCase(private val store: MicrophoneIntroStore) {
    operator fun invoke() = store.markShown()
}
