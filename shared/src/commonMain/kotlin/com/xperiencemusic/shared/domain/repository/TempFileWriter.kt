package com.xperiencemusic.shared.domain.repository

/** Writes bytes to the app's cache so a platform API that wants a file can open them. */
interface TempFileWriter {
    /** Returns a file URL. */
    fun write(fileName: String, bytes: ByteArray): String
}
