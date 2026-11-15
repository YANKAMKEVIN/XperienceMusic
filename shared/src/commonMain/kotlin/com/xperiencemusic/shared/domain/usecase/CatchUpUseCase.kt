package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.repository.RemoteLoadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * Fetches again whatever failed: the band's content (songs and setlists) and the other loads. A
 * phone waking up often has no network for its first seconds; without this, the app stayed
 * "hors connexion" until the member pulled to refresh.
 */
class CatchUpUseCase(
    private val observeSync: ObserveSyncStatusUseCase,
    private val loads: RemoteLoadStatus,
    private val refreshContent: RefreshContentUseCase,
) {
    /** True while something could not be fetched. */
    fun failures(): Flow<Boolean> =
        combine(observeSync(), loads.hasFailure) { sync, loadFailed -> sync == SyncStatus.FAILED || loadFailed }
            .distinctUntilChanged()

    suspend operator fun invoke() {
        if (observeSync().first() == SyncStatus.FAILED) refreshContent()
        if (loads.hasFailure.value) loads.retry()
    }
}
