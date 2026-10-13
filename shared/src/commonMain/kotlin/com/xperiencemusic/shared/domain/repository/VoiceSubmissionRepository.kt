package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import kotlinx.coroutines.flow.Flow

interface VoiceSubmissionRepository {
    /** [memberId]'s submissions, one per song. */
    fun observeMine(memberId: String): Flow<List<VoiceSubmission>>

    /** Everyone's submissions, for the super admin. */
    fun observeAll(): Flow<List<VoiceSubmission>>

    /**
     * Sends [memberId]'s part on [songId], replacing an earlier one and asking for a new review.
     * Returns the audio path it replaced, so the old file can be deleted.
     */
    suspend fun submit(memberId: String, songId: String, audioPath: String, durationSeconds: Int): String?

    suspend fun review(submissionId: String, status: ExerciseStatus, feedback: String?)
}
