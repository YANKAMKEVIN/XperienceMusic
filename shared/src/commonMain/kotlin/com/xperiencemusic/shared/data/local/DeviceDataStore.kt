package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.data.repository.InMemorySessionRepository
import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import com.xperiencemusic.shared.domain.repository.SessionResettable

/**
 * Wipes what the phone keeps of a member. [sessionScoped] lists everything holding account data
 * in memory (Koin gives every definition bound to [SessionResettable]); it is read at sign-out
 * only, so that nothing is built just for this store at start-up.
 */
class DeviceDataStore(
    private val snapshots: SnapshotStore,
    private val audioCache: AudioCache,
    private val files: FileStore,
    private val reminders: ReminderScheduler,
    private val sessionScoped: () -> List<SessionResettable> = { emptyList() },
) : LocalDataStore {
    override suspend fun clearAll() {
        sessionScoped().forEach { it.resetSession() }
        snapshots.clear()
        files.delete("voice")
        audioCache.clear()
        files.delete(DEMO_UPLOADS)
        files.delete(InMemorySessionRepository.DIRECTORY)
        files.delete(FileAnnouncedMonthsStore.DIRECTORY)
        // Someone else may sign in next: no more reminders about this member's services.
        runCatching { reminders.replaceAll(emptyList()) }
    }

    private companion object {
        const val DEMO_UPLOADS = "demo"
    }
}
