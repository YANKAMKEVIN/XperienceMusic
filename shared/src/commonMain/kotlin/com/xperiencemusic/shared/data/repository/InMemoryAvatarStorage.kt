package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.repository.AvatarStorage

/** Demo mode: keeps uploaded photos in memory for the session. */
class InMemoryAvatarStorage : AvatarStorage {
    private val files = mutableMapOf<String, ByteArray>()

    val paths: Set<String> get() = files.keys

    override suspend fun upload(path: String, jpeg: ByteArray) {
        files[path] = jpeg
    }

    override suspend fun delete(path: String) {
        files.remove(path)
    }

    override suspend fun download(path: String): ByteArray? = files[path]
}
