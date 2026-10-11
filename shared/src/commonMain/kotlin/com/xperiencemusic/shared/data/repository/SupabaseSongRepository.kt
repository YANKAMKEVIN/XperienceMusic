package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.mapper.toWriteDto
import com.xperiencemusic.shared.data.remote.dto.IdDto
import com.xperiencemusic.shared.data.remote.AUDIO_BUCKET
import com.xperiencemusic.shared.data.remote.dto.SongDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.tracking
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.repository.SongRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
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
) : SongRepository {
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
        val created = postgrestCall {
            client.from(SONGS).insert(draft.toWriteDto()) { select(Columns.list("id")) }.decodeSingle<IdDto>()
        }
        reload()
        return created.id
    }

    override suspend fun updateSong(id: String, draft: SongDraft) {
        postgrestCall {
            client.from(SONGS).update(draft.toWriteDto()) { filter { eq("id", id) } }
        }
        reload()
    }

    override suspend fun deleteSong(id: String) {
        val audioPaths = songs.value?.get(id)?.let { song ->
            song.recordings.map { it.audioPath } + song.voiceTracks.map { it.audioPath }
        }.orEmpty()
        // Rows of recordings, voice tracks and setlist entries go with the song (on delete cascade).
        postgrestCall { client.from(SONGS).delete { filter { eq("id", id) } } }
        if (audioPaths.isNotEmpty()) ignoringFailures { client.storage.from(AUDIO_BUCKET).delete(audioPaths) }
        reload()
    }

    override suspend fun addRecording(songId: String, recording: Recording, replacingId: String?) {
        postgrestCall {
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
        reload()
    }

    override suspend fun deleteRecording(songId: String, recordingId: String, newReferenceId: String?) {
        val deletedPath = postgrestCall {
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
        reload()
    }

    override suspend fun setReference(songId: String, recordingId: String) {
        postgrestCall {
            client.postgrest.rpc("set_reference_recording", buildJsonObject { put("p_recording_id", recordingId) })
        }
        reload()
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

    private suspend fun reload() = loading.withLock {
        val fetched = postgrestCall {
            client.from(SONGS).select(Columns.raw(SongDto.COLUMNS)).decodeList<SongDto>()
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
        const val SNAPSHOT = "songs"
    }
}
