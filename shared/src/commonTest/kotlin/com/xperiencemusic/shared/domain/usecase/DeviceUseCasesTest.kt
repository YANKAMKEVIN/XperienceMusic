package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.local.FileMicrophoneIntroStore
import com.xperiencemusic.shared.data.repository.InMemoryAvatarStorage
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceUseCasesTest {
    @Test
    fun `la photo d'un membre est rendue telle qu'enregistrée`() = runTest {
        val storage = InMemoryAvatarStorage()
        storage.upload("me/1.jpg", byteArrayOf(1, 2, 3))

        assertContentEquals(byteArrayOf(1, 2, 3), LoadAvatarPhotoUseCase(storage)("me/1.jpg"))
        assertNull(LoadAvatarPhotoUseCase(storage)("me/unknown.jpg"))
    }

    @Test
    fun `une photo injoignable laisse place aux initiales`() = runTest {
        val offline = object : AvatarStorage {
            override suspend fun upload(path: String, jpeg: ByteArray) = Unit
            override suspend fun delete(path: String) = Unit
            override suspend fun download(path: String): ByteArray? = error("offline")
        }

        assertNull(LoadAvatarPhotoUseCase(offline)("me/1.jpg"))
    }

    @Test
    fun `le micro n'est expliqué qu'une fois`() {
        val store = FileMicrophoneIntroStore(FakeFileStore())
        val wasExplained = WasMicrophoneExplainedUseCase(store)

        assertFalse(wasExplained())
        MarkMicrophoneExplainedUseCase(store)()
        assertTrue(wasExplained())
    }
}
