package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.ObjectiveRecording
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.ObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.repository.SessionResettable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Demo mode: the takes go to the in-memory storage, so they can be heard again during the session. */
class InMemoryObjectiveRecordingRepository(private val storage: AudioStorage) : ObjectiveRecordingRepository, SessionResettable {
    private val rows = MutableStateFlow<Map<String, List<ObjectiveRecording>>>(emptyMap())

    override fun resetSession() {
        rows.value = emptyMap()
    }

    override fun observe(memberId: String): Flow<List<ObjectiveRecording>> = rows.map { it[memberId].orEmpty() }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override suspend fun send(memberId: String, objectiveId: String, wav: ByteArray, seconds: Int) {
        require(seconds in 3..300 && wav.isNotEmpty())
        val path = "submissions/$memberId/objectives/${Uuid.random()}.wav"
        storage.upload(path, "audio/wav", wav)
        val value = ObjectiveRecording(memberId, objectiveId, path, seconds, Clock.System.now().toString())
        rows.value = rows.value + (memberId to (rows.value[memberId].orEmpty() + value))
    }
}
