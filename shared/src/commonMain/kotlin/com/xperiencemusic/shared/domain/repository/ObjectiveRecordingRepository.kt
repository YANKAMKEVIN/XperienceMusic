package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.ObjectiveRecording
import kotlinx.coroutines.flow.Flow

/** The takes members send for an objective; every earlier take stays available. */
interface ObjectiveRecordingRepository {
    fun observe(memberId: String): Flow<List<ObjectiveRecording>>

    suspend fun send(memberId: String, objectiveId: String, wav: ByteArray, seconds: Int)
}
