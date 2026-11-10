package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.data.remote.dto.ObjectiveRecordingDto
import com.xperiencemusic.shared.data.remote.dto.ObjectiveRecordingWriteDto
import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.domain.model.ObjectiveRecording
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.ObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.repository.SessionResettable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SupabaseObjectiveRecordingRepository(
    private val storage: AudioStorage,
    private val client: SupabaseClient,
    private val loads: RemoteLoadMonitor,
) : ObjectiveRecordingRepository, SessionResettable {
    private val rows = MutableStateFlow<Map<String, List<ObjectiveRecording>>>(emptyMap())
    private val session = SessionGuard()

    override fun resetSession() {
        session.reset()
        rows.value = emptyMap()
    }

    override fun observe(memberId: String): Flow<List<ObjectiveRecording>> = rows
        .onStart {
            loads.load("objective-audio-$memberId") {
                val fetched = session.call {
                    client.from(HISTORY)
                        .select { filter { eq("member_id", memberId) } }
                        .decodeList<ObjectiveRecordingDto>()
                }
                rows.value = rows.value + (memberId to fetched.map { it.toDomain() })
            }
        }
        .map { it[memberId].orEmpty() }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override suspend fun send(memberId: String, objectiveId: String, wav: ByteArray, seconds: Int) {
        require(seconds in 3..300 && wav.isNotEmpty())
        val started = session.stamp()
        val path = "submissions/$memberId/objectives/${Uuid.random()}.wav"
        storage.upload(path, "audio/wav", wav)
        try {
            session.ensureCurrent(started)
            client.from(LATEST).upsert(ObjectiveRecordingWriteDto(memberId, objectiveId, path, seconds)) {
                onConflict = "member_id,objective_id"
            }
        } catch (e: Exception) {
            // A lost response may follow a committed row. Only delete on a definite server refusal.
            if (e is PostgrestRestException) runCatching { storage.delete(path) }
            throw e
        }
        session.ensureCurrent(started)
        // Previous submitted takes remain available for listening to progress over time.
        val value = ObjectiveRecording(memberId, objectiveId, path, seconds, Clock.System.now().toString())
        rows.value = rows.value + (memberId to (rows.value[memberId].orEmpty() + value))
    }

    private companion object {
        const val LATEST = "objective_recordings"
        const val HISTORY = "objective_recording_history"
    }
}
