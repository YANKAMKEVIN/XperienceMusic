package com.xperiencemusic.shared.domain.model

/** An audio file picked on the device, read into memory before upload. */
class AudioFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    /** Null when the platform could not read the duration. */
    val durationSeconds: Int?,
) {
    val extension: String get() = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    companion object {
        /** Same limit as the storage bucket. */
        const val MAX_BYTES = 50 * 1024 * 1024
    }
}
