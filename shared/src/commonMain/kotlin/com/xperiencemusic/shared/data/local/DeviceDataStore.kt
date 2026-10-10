package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.LocalDataStore

class DeviceDataStore(
    private val snapshots: SnapshotStore,
    private val audioCache: AudioCache,
    private val files: FileStore,
) : LocalDataStore {
    override suspend fun clearAll() {
        snapshots.clear()
        audioCache.clear()
        files.delete(DEMO_UPLOADS)
    }

    private companion object {
        const val DEMO_UPLOADS = "demo"
    }
}
