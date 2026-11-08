package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.remote.dto.AudioPathDto
import com.xperiencemusic.shared.data.remote.dto.VoiceReviewDto
import com.xperiencemusic.shared.data.remote.dto.VoiceSubmissionDto
import com.xperiencemusic.shared.data.remote.dto.VoiceSubmissionUpsertDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.time.Clock

/**
 * Row level security decides what comes back: a singer gets their own submissions, the super admin
 * everyone's. Reloaded each time a screen starts watching, and after every change.
 */
class SupabaseVoiceSubmissionRepository(private val client: SupabaseClient,
    private val loads: com.xperiencemusic.shared.data.remote.RemoteLoadMonitor = com.xperiencemusic.shared.data.remote.RemoteLoadMonitor()) : VoiceSubmissionRepository, com.xperiencemusic.shared.domain.repository.SessionResettable {
    private var sessionGeneration = 0

    override fun resetSession() {
        sessionGeneration++
        all.value = null
    }

    private val all = MutableStateFlow<List<VoiceSubmission>?>(null)

    override fun observeMine(memberId: String): Flow<List<VoiceSubmission>> =
        observe().map { list -> list.filter { it.memberId == memberId } }

    override fun observeAll(): Flow<List<VoiceSubmission>> = observe()

    private fun observe(): Flow<List<VoiceSubmission>> =
        all.onStart { loads.load("VoiceSubmission") { reload() } }.filterNotNull()

    override suspend fun submit(memberId: String, songId: String, audioPath: String, durationSeconds: Int): String? {
        val previous = accountCall {
            client.from(TABLE).select(Columns.list("audio_path")) {
                filter {
                    eq("member_id", memberId)
                    eq("song_id", songId)
                }
            }.decodeSingleOrNull<AudioPathDto>()
        }
        accountCall {
            client.from(TABLE).upsert(
                VoiceSubmissionUpsertDto(memberId, songId, audioPath, durationSeconds, Clock.System.now().toString()),
            ) { onConflict = "member_id,song_id" }
        }
        reload()
        return previous?.audioPath?.takeIf { it != audioPath }
    }

    override suspend fun review(submissionId: String, status: ExerciseStatus, feedback: String?) {
        accountCall {
            client.from(TABLE).update(VoiceReviewDto(status.name, feedback, Clock.System.now().toString())) {
                filter { eq("id", submissionId) }
            }
        }
        reload()
    }

    private suspend fun reload() {
        all.value = accountCall {
            client.from(TABLE).select(Columns.raw(VoiceSubmissionDto.COLUMNS)) {
                order("submitted_at", Order.DESCENDING)
            }.decodeList<VoiceSubmissionDto>()
        }.map { it.toDomain() }
    }

    private companion object {
        const val TABLE = "voice_submissions"
    }
    /** Discard responses started under a previous account, including snapshot writes. */
    private suspend fun <T> accountCall(block: suspend () -> T): T {
        val generation = sessionGeneration
        val result = postgrestCall(block)
        if (generation != sessionGeneration) throw kotlin.coroutines.cancellation.CancellationException("Session ended")
        return result
    }

}
