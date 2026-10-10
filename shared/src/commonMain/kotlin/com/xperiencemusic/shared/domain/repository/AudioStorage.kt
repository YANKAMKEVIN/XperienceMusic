package com.xperiencemusic.shared.domain.repository

interface AudioStorage {
    /** Stores the file at [path] (relative to the audio bucket). */
    suspend fun upload(path: String, mimeType: String, bytes: ByteArray)

    suspend fun delete(path: String)
}
