package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileMicrophoneIntroStoreTest {
    @Test fun theExplanationIsShownOnceForThePhone() {
        val files = FakeFileStore()
        val store = FileMicrophoneIntroStore(files)
        assertFalse(store.wasShown())
        store.markShown()
        assertTrue(store.wasShown())
        // Signing out wipes the member's snapshots, not the phone's marker.
        SnapshotStore(files).clear()
        assertTrue(FileMicrophoneIntroStore(files).wasShown())
    }
}
