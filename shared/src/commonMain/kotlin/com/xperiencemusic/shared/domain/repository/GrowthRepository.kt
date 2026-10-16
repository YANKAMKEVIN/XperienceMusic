package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.LevelRequest
import com.xperiencemusic.shared.domain.model.ObjectiveCheck
import kotlinx.coroutines.flow.Flow

/**
 * Level-up requests and musicians' objectives. Server-side a member sees and sends only their
 * own; the super admin sees everyone's and answers.
 */
interface GrowthRepository {
    fun observeRequests(): Flow<List<LevelRequest>>

    fun observeChecks(): Flow<List<ObjectiveCheck>>

    /** Asks for [target], or asks again after a TO_REWORK. */
    suspend fun requestLevel(memberId: String, target: Level)

    /** VALIDATED moves the member up (the server does it). */
    suspend fun reviewRequest(requestId: String, status: ExerciseStatus, feedback: String?)

    suspend fun tickObjective(memberId: String, objectiveId: String)

    suspend fun untickObjective(memberId: String, objectiveId: String)

    suspend fun reviewObjective(memberId: String, objectiveId: String, status: ExerciseStatus, feedback: String?)
}
