package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.repository.AudioStorage

/** Demo mode: keeps uploaded files in memory for the session. */
class InMemoryAudioStorage : AudioStorage {
    private val files = mutableMapOf<String, ByteArray>()

    val paths: Set<String> get() = files.keys

    fun bytes(path: String): ByteArray? = files[path]

    override suspend fun upload(path: String, mimeType: String, bytes: ByteArray) {
        files[path] = bytes
    }

    override suspend fun delete(path: String) {
        files.remove(path)
    }
}
