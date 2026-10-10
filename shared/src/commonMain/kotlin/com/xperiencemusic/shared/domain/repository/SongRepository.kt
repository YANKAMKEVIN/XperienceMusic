package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Recording
import com.xperiencemusic.shared.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun observeSongs(): Flow<List<Song>>

    fun observeSong(id: String): Flow<Song?>

    suspend fun song(id: String): Song?

    /** Replaces the whole recording list of a song; business rules live in the use cases. */
    suspend fun saveRecordings(songId: String, recordings: List<Recording>)
}
