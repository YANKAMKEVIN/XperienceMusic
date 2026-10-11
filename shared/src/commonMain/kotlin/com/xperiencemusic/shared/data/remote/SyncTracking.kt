package com.xperiencemusic.shared.data.remote

import com.xperiencemusic.shared.domain.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow

/** Runs a server fetch, reflecting it in this status; failures still propagate. */
internal suspend fun MutableStateFlow<SyncStatus>.tracking(fetch: suspend () -> Unit) {
    value = SyncStatus.SYNCING
    try {
        fetch()
        value = SyncStatus.UP_TO_DATE
    } catch (e: Exception) {
        // Cancellation included: the fetch did not complete either way.
        value = SyncStatus.FAILED
        throw e
    }
}
