package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.repository.ReminderScheduler

class DeviceDataStore(
    private val snapshots: SnapshotStore,
    private val audioCache: AudioCache,
    private val files: FileStore,
    private val reminders: ReminderScheduler,
) : LocalDataStore {
    override suspend fun clearAll() {
        snapshots.clear()
        audioCache.clear()
        files.delete(DEMO_UPLOADS)
        files.delete(FileAnnouncedMonthsStore.DIRECTORY)
        // Someone else may sign in next: no more reminders about this member's services.
        runCatching { reminders.replaceAll(emptyList()) }
    }

    private companion object {
        const val DEMO_UPLOADS = "demo"
    }
}
