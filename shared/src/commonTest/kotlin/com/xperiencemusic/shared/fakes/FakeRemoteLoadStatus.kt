package com.xperiencemusic.shared.fakes

import com.xperiencemusic.shared.domain.repository.RemoteLoadStatus
import kotlinx.coroutines.flow.MutableStateFlow

/** Fails until [retry] is called, which counts the attempts and clears the failure. */
class FakeRemoteLoadStatus(failed: Boolean = false) : RemoteLoadStatus {
    override val hasFailure = MutableStateFlow(failed)
    var retries = 0
        private set

    override suspend fun retry() {
        retries++
        hasFailure.value = false
    }
}
