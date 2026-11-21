package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.dto.VoiceTrackWriteDto
import com.xperiencemusic.shared.domain.model.VoiceTrack
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.mapper.toWriteDto
import com.xperiencemusic.shared.data.remote.AUDIO_BUCKET
import com.xperiencemusic.shared.data.remote.allPages
import com.xperiencemusic.shared.data.remote.dto.IdDto
import com.xperiencemusic.shared.data.remote.dto.SongDto
import com.xperiencemusic.shared.data.remote.dto.SongLinkWriteDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.tracking
import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.data.session.accountCall
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.model.YouTubeLink
import com.xperiencemusic.shared.domain.repository.SessionResettable
import com.xperiencemusic.shared.domain.repository.SongChartLoader
import com.xperiencemusic.shared.domain.repository.SongRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The list of songs (titles, keys, recordings, links: no charts) is fetched page by page and kept
 * in memory; every change reloads it. The charts, most of the catalogue's weight, are fetched apart:
 * one song's when its page opens, every song's the first time a search reads the lyrics, and those
 * of the weeks downloaded ahead. Offline-first: the last fetched copies (list and charts) are shown
 * at once and refreshed from the server once per app session, and kept as is without network.
 */
class SupabaseSongRepository private constructor(
    private val client: SupabaseClient,
    private val snapshots: SnapshotStore,
    private val session: SessionGuard,
    private val chartStore: SupabaseSongCharts,
) : SongRepository, SessionResettable, SongChartLoader by chartStore {
    constructor(client: SupabaseClient, snapshots: SnapshotStore) : this(client, snapshots, SessionGuard())

    private constructor(client: SupabaseClient, snapshots: SnapshotStore, session: SessionGuard) :
        this(client, snapshots, session, SupabaseSongCharts(client, snapshots, session))

    override fun resetSession() {
        session.reset()
        songs.value = null
        chartStore.reset()
        hasRefreshed = false
    }

    private val songs = MutableStateFlow<Map<String, SongDto>?>(null)
    private val loading = Mutex()
    private var hasRefreshed = false
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override suspend fun refresh() {
        // A pull-to-refresh also renews the lyrics, the next time a search needs them.
        chartStore.markStale()
        _syncStatus.tracking { reload() }
    }

    override fun observeSongs(): Flow<List<Song>> =
        combine(songs.onStart { showSnapshotThenRefresh() }.filterNotNull(), chartStore.charts) { list, charts ->
            list.values.map { it.toDomain(charts[it.id]) }
        }

    override fun observeSong(id: String): Flow<Song?> = channelFlow {
        // The chart is fetched alongside: the copy on the phone, if any, shows meanwhile.
        launch { ignoringFailures { chartStore.loadCharts(listOf(id)) } }
        combine(songs.onStart { showSnapshotThenRefresh() }.filterNotNull(), chartStore.charts) { list, charts ->
            list[id]?.toDomain(charts[id])
        }.collect { send(it) }
    }.distinctUntilChanged()

    override suspend fun song(id: String): Song? {
        loadIfNeeded()
        return songs.value?.get(id)?.toDomain(chartStore.charts.value[id])
    }

    override suspend fun createSong(draft: SongDraft): String {
        val created = session.accountCall {
            client.from(SONGS).insert(draft.toWriteDto()) { select(Columns.list("id")) }.decodeSingle<IdDto>()
                .also { insertYouTubeLink(it.id, draft.youtubeUrl) }
        }
        chartStore.put(created.id, draft.chart)
        refreshAfterWrite()
        return created.id
    }

    override suspend fun updateSong(id: String, draft: SongDraft) {
        session.accountCall {
            client.from(SONGS).update(draft.toWriteDto()) { filter { eq("id", id) } }
            replaceYouTubeLink(id, draft.youtubeUrl)
        }
        chartStore.put(id, draft.chart)
        refreshAfterWrite()
    }

    /** A song has one YouTube link: the previous one goes, the new one (if any) takes its place. */
    private suspend fun replaceYouTubeLink(songId: String, url: String?) {
        client.from(SONG_LINKS).delete {
            filter {
                eq("song_id", songId)
                eq("label", YouTubeLink.LABEL)
            }
        }
        insertYouTubeLink(songId, url)
    }

    /** A new song has no link yet: there is nothing to delete first. */
    private suspend fun insertYouTubeLink(songId: String, url: String?) {
        if (url != null) client.from(SONG_LINKS).insert(SongLinkWriteDto(songId, YouTubeLink.LABEL, url))
    }

    override suspend fun deleteSong(id: String) {
        val audioPaths = songs.value?.get(id)?.let { song ->
            song.recordings.map { it.storagePath } + song.voiceTracks.map { it.storagePath }
        }.orEmpty()
        // Rows of recordings, voice tracks and setlist entries go with the song (on delete cascade).
        session.accountCall { client.from(SONGS).delete { filter { eq("id", id) } } }
        if (audioPaths.isNotEmpty()) ignoringFailures { client.storage.from(AUDIO_BUCKET).delete(audioPaths) }
        chartStore.put(id, null)
        refreshAfterWrite()
    }

    override suspend fun addRecording(songId: String, recording: Recording, replacingId: String?) {
        session.accountCall {
            client.postgrest.rpc(
                "add_recording",
                buildJsonObject {
                    put("p_id", recording.id)
                    put("p_song_id", songId)
                    put("p_title", recording.title)
                    put("p_recorded_on", recording.recordedOn.toString())
                    put("p_duration_seconds", recording.durationSeconds)
                    put("p_storage_path", recording.audioPath)
                    put("p_make_reference", recording.isReference)
                    put("p_replacing_id", replacingId)
                },
            )
        }
        refreshAfterWrite()
    }

    override suspend fun deleteRecording(songId: String, recordingId: String, newReferenceId: String?) {
        val deletedPath = session.accountCall {
            client.postgrest.rpc(
                "delete_recording",
                buildJsonObject {
                    put("p_recording_id", recordingId)
                    put("p_new_reference_id", newReferenceId)
                },
            ).decodeAs<String>()
        }
        // The row is gone either way; a leftover file only costs storage.
        ignoringFailures { client.storage.from(AUDIO_BUCKET).delete(deletedPath) }
        refreshAfterWrite()
    }

    override suspend fun setReference(songId: String, recordingId: String) {
        session.accountCall {
            client.postgrest.rpc("set_reference_recording", buildJsonObject { put("p_recording_id", recordingId) })
        }
        refreshAfterWrite()
    }

    override suspend fun setVoiceTrack(songId: String, track: VoiceTrack) {
        session.accountCall {
            client.from(VOICE_TRACKS).upsert(
                VoiceTrackWriteDto(track.id, songId, track.voicePart.name, track.audioPath),
            ) { onConflict = "song_id,voice_part" }
        }
        refreshAfterWrite()
    }

    private suspend fun loadIfNeeded() {
        if (songs.value == null) showSnapshot()
        if (songs.value == null) reload()
    }

    private suspend fun showSnapshotThenRefresh() {
        if (songs.value == null) showSnapshot()
        if (!hasRefreshed) ignoringFailures { _syncStatus.tracking { reload() } }
    }


    private fun showSnapshot() {
        val cached = snapshots.load(SNAPSHOT, ListSerializer(SongDto.serializer())) ?: return
        chartStore.restore(cached)
        publish(cached)
    }

    /**
     * Called after a write the server accepted: the write must stay a success even if this reload
     * fails (network drop), or the user retries and duplicates it. The failure shows in [syncStatus]
     * and the next refresh retries.
     */
    private suspend fun refreshAfterWrite() = ignoringFailures { _syncStatus.tracking { reload() } }

    private suspend fun reload() = loading.withLock {
        val fetched = session.accountCall {
            allPages { from, to ->
                client.from(SONGS).select(Columns.raw(SongDto.LIST_COLUMNS)) {
                    order("id", Order.ASCENDING)
                    range(from, to)
                }.decodeList<SongDto>()
            }
        }
        publish(fetched)
        snapshots.save(SNAPSHOT, ListSerializer(SongDto.serializer()), fetched)
        hasRefreshed = true
    }

    private fun publish(dtos: List<SongDto>) {
        // The charts are kept apart (see [SupabaseSongCharts]): the list holds none.
        songs.value = dtos.associate { it.id to it.copy(chart = null) }
    }

    private companion object {
        const val SONGS = "songs"
        const val VOICE_TRACKS = "voice_tracks"
        const val SONG_LINKS = "song_links"
        const val SNAPSHOT = "songs"
    }
}
