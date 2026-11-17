package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.remote.dto.VoiceTrackWriteDto
import com.xperiencemusic.shared.domain.model.VoiceTrack
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.mapper.toWriteDto
import com.xperiencemusic.shared.data.remote.AUDIO_BUCKET
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The whole library is small (a band's repertoire), so it is fetched in one query and kept in
 * memory; every change reloads it. Offline-first: the last fetched copy is shown at once and
 * refreshed from the server once per app session, and kept as is when there is no network.
 */
class SupabaseSongRepository(
    private val client: SupabaseClient,
    private val snapshots: SnapshotStore,
) : SongRepository, SessionResettable {
    private val session = SessionGuard()

    override fun resetSession() {
        session.reset()
        songs.value = null
        hasRefreshed = false
    }

    private val songs = MutableStateFlow<Map<String, Song>?>(null)
    private val loading = Mutex()
    private var hasRefreshed = false
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override suspend fun refresh() = _syncStatus.tracking { reload() }

    override fun observeSongs(): Flow<List<Song>> =
        songs.onStart { showSnapshotThenRefresh() }.filterNotNull().map { it.values.toList() }

    override fun observeSong(id: String): Flow<Song?> =
        songs.onStart { showSnapshotThenRefresh() }.filterNotNull().map { it[id] }

    override suspend fun song(id: String): Song? {
        loadIfNeeded()
        return songs.value?.get(id)
    }

    override suspend fun createSong(draft: SongDraft): String {
        val created = session.accountCall {
            client.from(SONGS).insert(draft.toWriteDto()) { select(Columns.list("id")) }.decodeSingle<IdDto>()
                .also { insertYouTubeLink(it.id, draft.youtubeUrl) }
        }
        refreshAfterWrite()
        return created.id
    }

    override suspend fun updateSong(id: String, draft: SongDraft) {
        session.accountCall {
            client.from(SONGS).update(draft.toWriteDto()) { filter { eq("id", id) } }
            replaceYouTubeLink(id, draft.youtubeUrl)
        }
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
            song.recordings.map { it.audioPath } + song.voiceTracks.map { it.audioPath }
        }.orEmpty()
        // Rows of recordings, voice tracks and setlist entries go with the song (on delete cascade).
        session.accountCall { client.from(SONGS).delete { filter { eq("id", id) } } }
        if (audioPaths.isNotEmpty()) ignoringFailures { client.storage.from(AUDIO_BUCKET).delete(audioPaths) }
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
        snapshots.load(SNAPSHOT, ListSerializer(SongDto.serializer()))?.let { cached -> publish(cached) }
    }

    /**
     * Called after a write the server accepted: the write must stay a success even if this reload
     * fails (network drop), or the user retries and duplicates it. The failure shows in [syncStatus]
     * and the next refresh retries.
     */
    private suspend fun refreshAfterWrite() = ignoringFailures { _syncStatus.tracking { reload() } }

    private suspend fun reload() = loading.withLock {
        val fetched = session.accountCall {
            // The server answers at most PAGE_SIZE rows per request: without pages, a catalogue of
            // two thousand songs stopped at the first thousand.
            buildList {
                do {
                    val page = client.from(SONGS).select(Columns.raw(SongDto.COLUMNS)) {
                        order("id", Order.ASCENDING)
                        range(size.toLong(), size + PAGE_SIZE - 1)
                    }.decodeList<SongDto>()
                    addAll(page)
                } while (page.isNotEmpty())
            }
        }
        publish(fetched)
        snapshots.save(SNAPSHOT, ListSerializer(SongDto.serializer()), fetched)
        hasRefreshed = true
    }

    private fun publish(dtos: List<SongDto>) {
        songs.value = dtos.associate { it.id to it.toDomain() }
    }

    private companion object {
        const val SONGS = "songs"
        /** Supabase's default row limit. A smaller server limit only costs more requests. */
        const val PAGE_SIZE = 1000L
        const val VOICE_TRACKS = "voice_tracks"
        const val SONG_LINKS = "song_links"
        const val SNAPSHOT = "songs"
    }
}
