package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.data.remote.dto.AudioPathDto
import com.xperiencemusic.shared.data.remote.dto.VoiceReviewDto
import com.xperiencemusic.shared.data.remote.dto.VoiceSubmissionDto
import com.xperiencemusic.shared.data.remote.dto.VoiceSubmissionUpsertDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.data.session.accountCall
import com.xperiencemusic.shared.domain.model.ExerciseStatus
import com.xperiencemusic.shared.domain.model.VoiceSubmission
import com.xperiencemusic.shared.domain.repository.SessionResettable
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Row level security decides what comes back: a singer gets their own submissions, the super admin
 * everyone's. Reloaded each time a screen starts watching, and after every change.
 */
class SupabaseVoiceSubmissionRepository(
    private val client: SupabaseClient,
    private val loads: RemoteLoadMonitor = RemoteLoadMonitor(),
) : VoiceSubmissionRepository, SessionResettable {
    private val session = SessionGuard()

    override fun resetSession() {
        session.reset()
        all.value = null
    }

    private val all = MutableStateFlow<List<VoiceSubmission>?>(null)

    override fun observeMine(memberId: String): Flow<List<VoiceSubmission>> =
        observe().map { list -> list.filter { it.memberId == memberId } }

    override fun observeAll(): Flow<List<VoiceSubmission>> = observe()

    private fun observe(): Flow<List<VoiceSubmission>> =
        all.onStart { loads.load("VoiceSubmission") { reload() } }.filterNotNull()

    override suspend fun submit(memberId: String, songId: String, audioPath: String, durationSeconds: Int): String? {
        val previous = session.accountCall {
            client.from(TABLE).select(Columns.list("audio_path")) {
                filter {
                    eq("member_id", memberId)
                    eq("song_id", songId)
                }
            }.decodeSingleOrNull<AudioPathDto>()
        }
        session.accountCall {
            client.from(TABLE).upsert(
                VoiceSubmissionUpsertDto(memberId, songId, audioPath, durationSeconds, Clock.System.now().toString()),
            ) { onConflict = "member_id,song_id" }
        }
        reload()
        return previous?.audioPath?.takeIf { it != audioPath }
    }

    override suspend fun review(submissionId: String, status: ExerciseStatus, feedback: String?) {
        session.accountCall {
            client.from(TABLE).update(VoiceReviewDto(status.name, feedback, Clock.System.now().toString())) {
                filter { eq("id", submissionId) }
            }
        }
        reload()
    }

    private suspend fun reload() {
        all.value = session.accountCall {
            client.from(TABLE).select(Columns.raw(VoiceSubmissionDto.COLUMNS)) {
                order("submitted_at", Order.DESCENDING)
            }.decodeList<VoiceSubmissionDto>()
        }.map { it.toDomain() }
    }

    private companion object {
        const val TABLE = "voice_submissions"
    }
}
