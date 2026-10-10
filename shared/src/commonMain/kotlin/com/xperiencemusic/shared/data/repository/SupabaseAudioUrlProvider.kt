package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.AUDIO_BUCKET
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.time.Duration.Companion.hours

/** The audio bucket is private: the player gets a link that works for a limited time. */
class SupabaseAudioUrlProvider(private val client: SupabaseClient) : AudioUrlProvider {
    override suspend fun playableUrl(audioPath: String): String =
        client.storage.from(AUDIO_BUCKET).createSignedUrl(audioPath, expiresIn = LINK_LIFETIME)

    private companion object {
        val LINK_LIFETIME = 3.hours
    }
}
