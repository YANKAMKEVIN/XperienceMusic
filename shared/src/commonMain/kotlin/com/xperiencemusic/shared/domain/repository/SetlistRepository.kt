package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SetlistRepository {
    val syncStatus: StateFlow<SyncStatus>

    /** Fetches the latest data from the server; throws when it cannot be reached. */
    suspend fun refresh()

    /** The most recently published setlist. */
    fun observeLatest(): Flow<Setlist?>

    suspend fun publish(setlist: Setlist)
}
