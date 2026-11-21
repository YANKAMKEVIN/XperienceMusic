package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.remote.allPages
import com.xperiencemusic.shared.data.remote.dto.SongChartDto
import com.xperiencemusic.shared.data.remote.dto.SongDto
import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.data.session.accountCall
import com.xperiencemusic.shared.domain.repository.SongChartLoader
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * The charts of the songs, kept apart from the list (see [SupabaseSongRepository]): they are most of
 * the catalogue's weight. They come one song at a time, when its page opens, or all at once, the
 * first time a search reads the lyrics; what was fetched stays on the phone for offline use.
 */
internal class SupabaseSongCharts(
    private val client: SupabaseClient,
    private val snapshots: SnapshotStore,
    private val session: SessionGuard,
) : SongChartLoader {
    private val _charts = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Charts by song id: those fetched so far, or kept on the phone. */
    val charts: StateFlow<Map<String, String>> = _charts.asStateFlow()
    private val loadingAll = Mutex()
    /** Every chart was fetched in this app session (see [loadLyrics]). */
    private var hasAll = false

    fun reset() {
        _charts.value = emptyMap()
        hasAll = false
    }

    /** The next [loadLyrics] fetches every chart again (a pull-to-refresh). */
    fun markStale() {
        hasAll = false
    }

    /**
     * Shows the charts kept on the phone. [list] is the saved list: earlier versions of the app kept
     * the charts in it, and they move to their own copy before the next reload saves the list
     * without them.
     */
    fun restore(list: List<SongDto>) {
        if (_charts.value.isNotEmpty()) return
        val saved = snapshots.load(SNAPSHOT, serializer)
        if (saved != null) _charts.value = saved
        else publish(list.mapNotNull { dto -> dto.chart?.let { dto.id to it } }.toMap())
    }

    /** A chart the server just accepted; null when the song is gone. */
    fun put(songId: String, chart: String?) =
        publish(if (chart == null) _charts.value - songId else _charts.value + (songId to chart))

    override suspend fun loadLyrics() = loadingAll.withLock {
        if (hasAll) return@withLock
        val fetched = session.accountCall {
            allPages { from, to ->
                client.from(SONGS).select(Columns.raw(SongChartDto.COLUMNS)) {
                    order("id", Order.ASCENDING)
                    range(from, to)
                }.decodeList<SongChartDto>()
            }
        }
        // Every chart, fresh: those of deleted songs go.
        publish(fetched.associate { it.id to it.chart })
        hasAll = true
    }

    override suspend fun loadCharts(ids: Collection<String>) {
        val wanted = ids.distinct()
        if (wanted.isEmpty()) return
        val fetched = session.accountCall {
            client.from(SONGS).select(Columns.raw(SongChartDto.COLUMNS)) {
                filter { isIn("id", wanted) }
            }.decodeList<SongChartDto>()
        }
        publish(_charts.value + fetched.associate { it.id to it.chart })
    }

    /** Shows [next] and keeps it on the phone, unless nothing changed: the charts weigh megabytes. */
    private fun publish(next: Map<String, String>) {
        var changed = false
        _charts.update { current -> if (current == next) current else next.also { changed = true } }
        if (changed) snapshots.save(SNAPSHOT, serializer, next)
    }

    private companion object {
        const val SONGS = "songs"
        const val SNAPSHOT = "song_charts"
        val serializer = MapSerializer(String.serializer(), String.serializer())
    }
}
