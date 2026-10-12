package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface SetlistRepository {
    val syncStatus: StateFlow<SyncStatus>

    /** Fetches the latest data from the server; throws when it cannot be reached. */
    suspend fun refresh()

    /** Every setlist kept, past weeks and weeks sent ahead, by service date. */
    fun observeSetlists(): Flow<List<Setlist>>

    /** Publishes the setlist of its service date, replacing the one already there. */
    suspend fun publish(setlist: Setlist)

    suspend fun delete(serviceDate: LocalDate)
}
