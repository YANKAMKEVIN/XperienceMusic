package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class InMemorySetlistRepository(initial: List<Setlist> = emptyList()) : SetlistRepository {
    override val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.UP_TO_DATE)

    override suspend fun refresh() = Unit

    private val setlists = MutableStateFlow(initial.sortedBy { it.serviceDate })

    override fun observeSetlists(): Flow<List<Setlist>> = setlists.asStateFlow()

    override suspend fun publish(setlist: Setlist) {
        val stored = setlist.copy(id = setlist.id.ifEmpty { "setlist-${setlist.serviceDate}" })
        setlists.update { all -> (all.filterNot { it.serviceDate == setlist.serviceDate } + stored).sortedBy { it.serviceDate } }
    }

    override suspend fun delete(serviceDate: LocalDate) {
        setlists.update { all -> all.filterNot { it.serviceDate == serviceDate } }
    }
}
