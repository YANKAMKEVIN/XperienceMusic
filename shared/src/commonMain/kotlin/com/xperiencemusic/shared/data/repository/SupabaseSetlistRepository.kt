package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.remote.dto.SetlistDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.Setlist
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Offline-first like the songs: last copy at once, refreshed once per app session. */
class SupabaseSetlistRepository(
    private val client: SupabaseClient,
    private val snapshots: SnapshotStore,
) : SetlistRepository {
    /** Wrapped so "loaded, no setlist yet" differs from "not loaded". */
    private val latest = MutableStateFlow<Loaded?>(null)
    private var hasRefreshed = false

    private data class Loaded(val setlist: Setlist?)

    override fun observeLatest(): Flow<Setlist?> =
        latest.onStart {
            if (latest.value == null) {
                snapshots.load(SNAPSHOT, ListSerializer(SetlistDto.serializer()))
                    ?.let { cached -> latest.value = Loaded(cached.firstOrNull()?.toDomain()) }
            }
            if (!hasRefreshed) ignoringFailures { reload() }
        }.filterNotNull().map { it.setlist }

    override suspend fun publish(setlist: Setlist) {
        postgrestCall {
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

    private suspend fun reload() {
        val fetched = postgrestCall {
            client.from(SETLISTS).select(Columns.raw(SetlistDto.COLUMNS)) {
                order("published_at", Order.DESCENDING)
                limit(1)
            }.decodeList<SetlistDto>()
        }
        latest.value = Loaded(fetched.firstOrNull()?.toDomain())
        snapshots.save(SNAPSHOT, ListSerializer(SetlistDto.serializer()), fetched)
        hasRefreshed = true
    }

    private companion object {
        const val SETLISTS = "setlists"
        const val SNAPSHOT = "setlist"
    }
}
