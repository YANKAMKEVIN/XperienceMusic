package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.remote.dto.SetlistDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.tracking
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Offline-first like the songs: last copy at once, refreshed once per app session. */
class SupabaseSetlistRepository(
    private val client: SupabaseClient,
    private val snapshots: SnapshotStore,
) : SetlistRepository, com.xperiencemusic.shared.domain.repository.SessionResettable {
    private var sessionGeneration = 0

    override fun resetSession() {
        sessionGeneration++
        setlists.value = null
        hasRefreshed = false
    }

    /** Null until loaded, so "no setlist yet" differs from "not loaded". */
    private val setlists = MutableStateFlow<List<Setlist>?>(null)
    private var hasRefreshed = false
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override suspend fun refresh() = _syncStatus.tracking { reload() }

    override fun observeSetlists(): Flow<List<Setlist>> =
        setlists.onStart {
            if (setlists.value == null) {
                snapshots.load(SNAPSHOT, ListSerializer(SetlistDto.serializer()))
                    ?.let { cached -> setlists.value = cached.toDomainSorted() }
            }
            if (!hasRefreshed) ignoringFailures { _syncStatus.tracking { reload() } }
        }.filterNotNull()

    override suspend fun publish(setlist: Setlist) {
        accountCall {
            client.postgrest.rpc(
                "publish_setlist",
                buildJsonObject {
                    put("p_rehearsal_date", setlist.rehearsalDate.toString())
                    put("p_service_date", setlist.serviceDate.toString())
                    put("p_message", setlist.message)
                    putJsonArray("p_song_ids") { setlist.songIds.forEach { add(it) } }
                },
            )
        }
        reload()
    }

    override suspend fun delete(serviceDate: LocalDate) {
        accountCall {
            client.postgrest.rpc("delete_setlist", buildJsonObject { put("p_service_date", serviceDate.toString()) })
        }
        reload()
    }

    private suspend fun reload() {
        val fetched = accountCall {
            client.from(SETLISTS).select(Columns.raw(SetlistDto.COLUMNS)) {
                // Every week ahead, and about six months of history.
                order("service_date", Order.DESCENDING)
                limit(KEPT_WEEKS)
            }.decodeList<SetlistDto>()
        }
        setlists.value = fetched.toDomainSorted()
        snapshots.save(SNAPSHOT, ListSerializer(SetlistDto.serializer()), fetched)
        hasRefreshed = true
    }

    private companion object {
        const val SETLISTS = "setlists"
        const val SNAPSHOT = "setlists"
        const val KEPT_WEEKS = 40L
    }

    private fun List<SetlistDto>.toDomainSorted() = map { it.toDomain() }.sortedBy { it.serviceDate }
    /** Discard responses started under a previous account, including snapshot writes. */
    private suspend fun <T> accountCall(block: suspend () -> T): T {
        val generation = sessionGeneration
        val result = postgrestCall(block)
        if (generation != sessionGeneration) throw kotlin.coroutines.cancellation.CancellationException("Session ended")
        return result
    }

}
