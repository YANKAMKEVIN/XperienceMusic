package com.xperiencemusic.shared.fakes

import com.xperiencemusic.shared.data.repository.InMemorySongRepository
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.io.IOException

/**
 * Songs whose charts are not on the phone until asked for, like the list loaded from the server:
 * [loadLyrics] brings every chart, [loadCharts] some.
 */
class LazyChartsSongRepository(
    vararg songs: Song,
    private val inner: InMemorySongRepository = InMemorySongRepository(songs.toList()),
) : SongRepository by inner {
    private val loaded = MutableStateFlow<Set<String>?>(emptySet())
    var lyricsLoads = 0
        private set
    val chartLoads = mutableListOf<Collection<String>>()
    var isOffline = false

    override fun observeSongs(): Flow<List<Song>> = combine(inner.observeSongs(), loaded) { songs, loaded ->
        songs.map { if (loaded == null || it.id in loaded) it else it.copy(chart = null) }
    }

    /** Unlike the real repository, observing a song does not fetch its chart: tests call [loadCharts]. */
    override fun observeSong(id: String): Flow<Song?> = observeSongs().map { songs -> songs.firstOrNull { it.id == id } }

    override suspend fun loadLyrics() {
        lyricsLoads++
        if (isOffline) throw IOException("offline")
        loaded.value = null
    }

    override suspend fun loadCharts(ids: Collection<String>) {
        chartLoads += ids
        if (isOffline) throw IOException("offline")
        loaded.value = loaded.value?.plus(ids)
    }
}
