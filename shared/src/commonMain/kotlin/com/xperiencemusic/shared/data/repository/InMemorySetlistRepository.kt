package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemorySetlistRepository(initial: Setlist?) : SetlistRepository {
    private val latest = MutableStateFlow(initial)

    override fun observeLatest(): Flow<Setlist?> = latest.asStateFlow()

    override suspend fun publish(setlist: Setlist) {
        latest.value = setlist
    }
}
