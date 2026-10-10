package com.xperiencemusic.shared.domain.repository

/** Turns a stored audio path into a URL the platform player can open. */
fun interface AudioUrlProvider {
    suspend fun playableUrl(audioPath: String): String
}
