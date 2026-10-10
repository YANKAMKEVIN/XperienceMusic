package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.data.remote.dto.ProfileDto
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SnapshotStoreTest {
    private val files = FakeFileStore()
    private val snapshots = SnapshotStore(files)
    private val profile = ProfileDto("u1", "g@b.fr", "MEMBER", "Grâce", "SINGER", "ALTO", "PRO")

    @Test
    fun `une copie enregistrée se relit à l'identique`() {
        snapshots.save("profile", ProfileDto.serializer(), profile)
        assertEquals(profile, snapshots.load("profile", ProfileDto.serializer()))
    }

    @Test
    fun `une copie abîmée est ignorée plutôt que de planter`() {
        files.writeText("snapshots/profile.json", "{pas du json")
        assertNull(snapshots.load("profile", ProfileDto.serializer()))
    }

    @Test
    fun `clear efface toutes les copies`() {
        snapshots.save("profile", ProfileDto.serializer(), profile)
        snapshots.clear()
        assertNull(snapshots.load("profile", ProfileDto.serializer()))
    }
}
