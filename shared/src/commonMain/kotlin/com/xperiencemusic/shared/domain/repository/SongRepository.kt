package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SongDraft
import kotlinx.coroutines.flow.Flow

/**
 * Recording changes are single operations so a backend can apply them atomically. The use cases
 * decide what each operation does (which recording becomes the reference…); the backend re-checks.
 */
interface SongRepository {
    fun observeSongs(): Flow<List<Song>>

    fun observeSong(id: String): Flow<Song?>

    suspend fun song(id: String): Song?

    /** Returns the new song's id. */
    suspend fun createSong(draft: SongDraft): String

    suspend fun updateSong(id: String, draft: SongDraft)

    /** Adds [recording], removing [replacingId] in the same step when set. */
    suspend fun addRecording(songId: String, recording: Recording, replacingId: String?)

    /** Deletes a recording and, when set, makes [newReferenceId] the reference. */
    suspend fun deleteRecording(songId: String, recordingId: String, newReferenceId: String?)

    suspend fun setReference(songId: String, recordingId: String)
}
