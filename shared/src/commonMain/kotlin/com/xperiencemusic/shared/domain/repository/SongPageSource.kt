package com.xperiencemusic.shared.domain.repository

/** Reads the ChordPro text of one song page on accords.app. */
fun interface SongPageSource {
    /**
     * The ChordPro of the page at [pageUrl], or null when the page holds no song. Throws on network
     * failure.
     */
    suspend fun chordPro(pageUrl: String): String?
}
