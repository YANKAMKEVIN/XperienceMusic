package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.AVATARS_BUCKET
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import com.xperiencemusic.shared.domain.repository.SessionResettable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException

/**
 * Photos are fetched once per run and kept in memory: each photo has its own path, so a kept copy
 * is never stale. Offline, the circle falls back to the member's initials. Signing out drops the
 * copies, so the next account never sees them.
 */
class SupabaseAvatarStorage(private val client: SupabaseClient) : AvatarStorage, SessionResettable {
    private val cache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())

    override suspend fun upload(path: String, jpeg: ByteArray) {
        client.storage.from(AVATARS_BUCKET).upload(path, jpeg) {
            contentType = ContentType.Image.JPEG
            upsert = false
        }
        cache.update { it + (path to jpeg) }
    }

    override suspend fun delete(path: String) {
        client.storage.from(AVATARS_BUCKET).delete(path)
        cache.update { it - path }
    }

    override suspend fun download(path: String): ByteArray? {
        cache.value[path]?.let { return it }
        return try {
            client.storage.from(AVATARS_BUCKET).downloadAuthenticated(path).also { bytes ->
                cache.update { it + (path to bytes) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    override fun resetSession() {
        cache.value = emptyMap()
    }
}
