package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryVoiceSubmissionRepository(
    private val sessions: SessionRepository,
    private val songs: SongRepository,
    private val dates: DateProvider,
    initial: List<VoiceSubmission> = emptyList(),
) : VoiceSubmissionRepository {
    private val all = MutableStateFlow(initial)
    private var nextId = 1

    override fun observeMine(memberId: String): Flow<List<VoiceSubmission>> =
        all.map { list -> list.filter { it.memberId == memberId } }

    override fun observeAll(): Flow<List<VoiceSubmission>> = all

    override suspend fun submit(memberId: String, songId: String, audioPath: String, durationSeconds: Int): String? {
        val previous = all.value.firstOrNull { it.memberId == memberId && it.songId == songId }
        val member = sessions.currentMember()
        val submission = VoiceSubmission(
            id = previous?.id ?: "submission-${nextId++}",
            memberId = memberId,
            memberName = member?.profile?.displayName.orEmpty(),
            voicePart = member?.profile?.voicePart,
            songId = songId,
            songTitle = songs.observeSongs().first().firstOrNull { it.id == songId }?.title.orEmpty(),
            audioPath = audioPath,
            durationSeconds = durationSeconds,
            submittedOn = dates.today(),
            status = ExerciseStatus.SUBMITTED,
            feedback = null,
        )
        all.update { list -> listOf(submission) + list.filterNot { it.id == submission.id } }
        return previous?.audioPath
    }

    override suspend fun review(submissionId: String, status: ExerciseStatus, feedback: String?) {
        all.update { list -> list.map { if (it.id == submissionId) it.copy(status = status, feedback = feedback) else it } }
    }
}
