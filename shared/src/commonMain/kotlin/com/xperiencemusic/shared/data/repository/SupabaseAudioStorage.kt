package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.AUDIO_BUCKET
import com.xperiencemusic.shared.domain.repository.AudioStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType

class SupabaseAudioStorage(private val client: SupabaseClient) : AudioStorage {
    override suspend fun upload(path: String, mimeType: String, bytes: ByteArray) {
        client.storage.from(AUDIO_BUCKET).upload(path, bytes) {
            contentType = ContentType.parse(mimeType)
            upsert = false
        }
    }

    override suspend fun delete(path: String) {
        client.storage.from(AUDIO_BUCKET).delete(path)
    }
}
