package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** One status for the band's content: failed if any part failed, syncing if any part is. */
class ObserveSyncStatusUseCase(
    private val songs: SongRepository,
    private val setlists: SetlistRepository,
) {
    operator fun invoke(): Flow<SyncStatus> = combine(songs.syncStatus, setlists.syncStatus) { a, b ->
        when {
            a == SyncStatus.FAILED || b == SyncStatus.FAILED -> SyncStatus.FAILED
            a == SyncStatus.SYNCING || b == SyncStatus.SYNCING -> SyncStatus.SYNCING
            a == SyncStatus.UP_TO_DATE && b == SyncStatus.UP_TO_DATE -> SyncStatus.UP_TO_DATE
            else -> SyncStatus.IDLE
        }
    }
}
