package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import com.xperiencemusic.shared.fakes.FakeAudioCache
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceDataStoreTest {
    @Test
    fun signOutClearsPrivateFilesMemoryAudioAndNotifications() = runTest {
        val files = FakeFileStore()
        files.writeText("voice/take.wav", "private")
        files.writeText("snapshots/profile.json", "private")
        val audio = FakeAudioCache(setOf("song.m4a"))
        var reset = false
        var remindersCleared = false
        val reminders = object : ReminderScheduler {
            override suspend fun replaceAll(reminders: List<Reminder>) {
                remindersCleared = reminders.isEmpty()
            }
        }
        DeviceDataStore(SnapshotStore(files), audio, files, reminders) { reset = true }.clearAll()
        assertTrue(reset)
        assertTrue(files.files.isEmpty())
        assertTrue(audio.cachedPaths.value.isEmpty())
        assertTrue(remindersCleared)
    }
}
