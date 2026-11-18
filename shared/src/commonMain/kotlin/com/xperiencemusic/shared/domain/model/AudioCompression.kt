package com.xperiencemusic.shared.domain.model

import kotlin.math.min

/**
 * When a picked audio file is re-encoded before upload, and how hard. A phone's recorder writes
 * about 2 MB a minute: a whole rehearsal would not fit under [AudioFile.MAX_BYTES]. Mono AAC at
 * [TARGET_BITRATE] is plenty to rehearse from and keeps an hour near 30 MB.
 */
object AudioCompression {
    /** Small files go as they are: re-encoding would only cost quality and time. */
    const val COMPRESS_ABOVE_BYTES = 10L * 1024 * 1024
    const val TARGET_BITRATE = 64_000
    /** Below this, voices start to sound underwater. */
    const val MIN_BITRATE = 32_000

    /** What the compressed file aims for, leaving room under the storage limit for the container. */
    private const val BUDGET_BYTES = 45L * 1024 * 1024
    private const val BITS_PER_BYTE = 8
    /** A file already close to the target is not worth re-encoding. */
    private const val WORTH_IT_RATIO = 1.25

    /** The bitrate to re-encode at, in bits per second, or null to upload the file as it is. */
    fun targetBitrate(sizeBytes: Long, durationSeconds: Int?): Int? {
        if (sizeBytes <= COMPRESS_ABOVE_BYTES) return null
        val seconds = durationSeconds?.takeIf { it > 0 } ?: return TARGET_BITRATE
        val fitting = BUDGET_BYTES * BITS_PER_BYTE / seconds
        val target = min(TARGET_BITRATE.toLong(), fitting).coerceAtLeast(MIN_BITRATE.toLong()).toInt()
        val current = sizeBytes * BITS_PER_BYTE / seconds
        return target.takeIf { current > it * WORTH_IT_RATIO }
    }
}
