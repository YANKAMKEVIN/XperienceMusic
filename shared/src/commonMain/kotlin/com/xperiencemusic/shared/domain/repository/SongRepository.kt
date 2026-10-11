package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SongDraft
import com.xperiencemusic.shared.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Recording changes are single operations so a backend can apply them atomically. The use cases
 * decide what each operation does (which recording becomes the reference…); the backend re-checks.
 */
interface SongRepository {
    val syncStatus: StateFlow<SyncStatus>

    /** Fetches the latest data from the server; throws when it cannot be reached. */
    suspend fun refresh()

    fun observeSongs(): Flow<List<Song>>

    fun observeSong(id: String): Flow<Song?>

    suspend fun song(id: String): Song?

    /** Returns the new song's id. */
    suspend fun createSong(draft: SongDraft): String

    suspend fun updateSong(id: String, draft: SongDraft)

    /** Deletes the song with its recordings and voice tracks, audio files included. */
    suspend fun deleteSong(id: String)

    /** Adds [recording], removing [replacingId] in the same step when set. */
    suspend fun addRecording(songId: String, recording: Recording, replacingId: String?)

    /** Deletes a recording and, when set, makes [newReferenceId] the reference. */
    suspend fun deleteRecording(songId: String, recordingId: String, newReferenceId: String?)

    suspend fun setReference(songId: String, recordingId: String)
}
