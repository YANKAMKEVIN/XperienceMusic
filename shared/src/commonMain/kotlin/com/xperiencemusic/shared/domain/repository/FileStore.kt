package com.xperiencemusic.shared.domain.repository

/** The app's private storage. Paths are relative ("cache/songs.json", "audio/x.m4a"). */
interface FileStore {
    /** Writes [bytes] and returns a file URL the platform player can open. */
    fun write(relativePath: String, bytes: ByteArray): String

    /** File URL of an existing file, or null. */
    fun fileUrl(relativePath: String): String?

    fun readText(relativePath: String): String?

    fun writeText(relativePath: String, text: String)

    /** Names of the files directly inside [directory]; empty when it does not exist. */
    fun list(directory: String): List<String>

    /** Deletes a file, or a directory and everything in it. */
    fun delete(relativePath: String)
}
