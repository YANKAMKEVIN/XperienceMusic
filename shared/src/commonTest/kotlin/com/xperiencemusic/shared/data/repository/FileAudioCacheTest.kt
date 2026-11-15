package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.fakes.FakeFileStore
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Audio files reach 50 MB: writing and deleting them must not run on the caller's (main) thread. */
class FileAudioCacheTest {
    /** Runs blocks in place, remembering whether code is currently running "on" it. */
    private class RecordingDispatcher : CoroutineDispatcher() {
        var isRunning = false
        override fun isDispatchNeeded(context: CoroutineContext) = true
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val was = isRunning
            isRunning = true
            try { block.run() } finally { isRunning = was }
        }
    }

    private val io = RecordingDispatcher()
    private val operationsOffIo = mutableListOf<String>()

    private val store = FakeFileStore()
    private val files = object : FileStore by store {
        override fun write(relativePath: String, bytes: ByteArray): String {
            if (!io.isRunning) operationsOffIo += "write $relativePath"
            return store.write(relativePath, bytes)
        }

        override fun delete(relativePath: String) {
            if (!io.isRunning) operationsOffIo += "delete $relativePath"
            store.delete(relativePath)
        }
    }

    private val client = createSupabaseClient(supabaseUrl = "https://test.supabase.co", supabaseKey = "test-key") {
        httpEngine = MockEngine {
            respond(byteArrayOf(1, 2, 3), HttpStatusCode.OK, headersOf("Content-Type", "audio/mp4"))
        }
        // The default resumable-upload cache needs an Android context.
        install(Storage) { resumable { cache = MemoryResumableCache() } }
    }

    @Test
    fun `the downloaded file is written on the I-O dispatcher`() = runTest {
        val cache = FileAudioCache(client, files, io)

        cache.download("songs/s1/r1.m4a")

        assertEquals(emptyList(), operationsOffIo)
        assertTrue("songs/s1/r1.m4a" in cache.cachedPaths.value)
        assertEquals("file:///app/audio/songs__s1__r1.m4a", cache.localUrl("songs/s1/r1.m4a"))
    }

    @Test
    fun `clearing deletes the files on the I-O dispatcher`() = runTest {
        val cache = FileAudioCache(client, files, io)
        cache.download("songs/s1/r1.m4a")

        cache.clear()

        assertEquals(emptyList(), operationsOffIo)
        assertTrue(cache.cachedPaths.value.isEmpty())
    }
}
