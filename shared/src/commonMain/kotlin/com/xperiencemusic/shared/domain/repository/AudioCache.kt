package com.xperiencemusic.shared.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Audio files kept on the phone for listening without network. */
interface AudioCache {
    /** Audio paths available offline. */
    val cachedPaths: StateFlow<Set<String>>

    /** File URL of a downloaded file, or null. */
    fun localUrl(audioPath: String): String?

    suspend fun download(audioPath: String)

    /** Removes every downloaded file. */
    suspend fun clear()
}
