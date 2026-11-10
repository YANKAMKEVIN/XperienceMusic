package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.AVATARS_BUCKET
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Photos are fetched once per run and kept in memory: each photo has its own path, so a kept copy
 * is never stale. Offline, the circle falls back to the member's initials.
 */
class SupabaseAvatarStorage(private val client: SupabaseClient) : AvatarStorage {
    private val cache = mutableMapOf<String, ByteArray>()
    private val lock = Mutex()

    override suspend fun upload(path: String, jpeg: ByteArray) {
        client.storage.from(AVATARS_BUCKET).upload(path, jpeg) {
            contentType = ContentType.Image.JPEG
            upsert = false
        }
        lock.withLock { cache[path] = jpeg }
    }

    override suspend fun delete(path: String) {
        client.storage.from(AVATARS_BUCKET).delete(path)
        lock.withLock { cache.remove(path) }
    }

    override suspend fun download(path: String): ByteArray? {
        lock.withLock { cache[path] }?.let { return it }
        return try {
            client.storage.from(AVATARS_BUCKET).downloadAuthenticated(path).also { bytes ->
                lock.withLock { cache[path] = bytes }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
