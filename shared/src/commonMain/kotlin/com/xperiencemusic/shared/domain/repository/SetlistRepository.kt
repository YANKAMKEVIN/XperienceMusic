package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Setlist
import kotlinx.coroutines.flow.Flow

interface SetlistRepository {
    /** The most recently published setlist. */
    fun observeLatest(): Flow<Setlist?>

    suspend fun publish(setlist: Setlist)
}
