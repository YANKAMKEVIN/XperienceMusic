package com.xperiencemusic.shared.data.remote

/**
 * Supabase's default row limit: the server answers at most this many rows per request. A smaller
 * server limit only costs more requests.
 */
internal const val PAGE_SIZE = 1000L

/**
 * Asks for [page] after page of rows until one comes back empty: without pages, a catalogue of two
 * thousand songs stopped at the first thousand.
 */
internal suspend fun <T> allPages(page: suspend (from: Long, to: Long) -> List<T>): List<T> = buildList {
    do {
        val rows = page(size.toLong(), size + PAGE_SIZE - 1)
        addAll(rows)
    } while (rows.isNotEmpty())
}
