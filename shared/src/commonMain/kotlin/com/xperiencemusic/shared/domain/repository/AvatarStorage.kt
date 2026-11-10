package com.xperiencemusic.shared.domain.repository

/** Member photos, stored as small square JPEGs. Paths are relative to the avatars bucket. */
interface AvatarStorage {
    suspend fun upload(path: String, jpeg: ByteArray)

    suspend fun delete(path: String)

    /** Null when the photo cannot be fetched (offline, deleted). */
    suspend fun download(path: String): ByteArray?
}
