package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileDevicePreferencesTest {
    @Test
    fun `un réglage écrit est relu après un redémarrage`() {
        val files = FakeFileStore()
        FileDevicePreferences(files).apply {
            write("chart.notation", "SOLFEGE")
            write("stage.scale", "2.0")
            write("chart.notation", "LETTERS")
        }
        val restarted = FileDevicePreferences(files)
        assertEquals("LETTERS", restarted.read("chart.notation"))
        assertEquals("2.0", restarted.read("stage.scale"))
        assertNull(restarted.read("unknown"))
    }

    @Test
    fun `se déconnecter garde les réglages du téléphone`() {
        val files = FakeFileStore()
        FileDevicePreferences(files).write("chart.notation", "SOLFEGE")
        SnapshotStore(files).clear()
        assertEquals("SOLFEGE", FileDevicePreferences(files).read("chart.notation"))
    }
}
