package com.xperiencemusic.shared.domain.repository

/**
 * The list of songs is loaded without the charts (see [SongRepository.observeSongs]); these bring
 * them to the phone when they are needed.
 */
interface SongChartLoader {
    /**
     * Fetches the chart of every song, for a search in the lyrics; done once per app session
     * (a pull-to-refresh starts again). Throws when the server cannot be reached.
     */
    suspend fun loadLyrics()

    /** Fetches the charts of [ids], to read them offline (the weeks downloaded ahead). Throws on failure. */
    suspend fun loadCharts(ids: Collection<String>)
}
