package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.mapper.toDto
import com.xperiencemusic.shared.data.remote.dto.LearningAttemptDto
import com.xperiencemusic.shared.domain.model.LearningAttempt
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SupabaseLearningRemote(private val client: SupabaseClient) : LearningRemote {
    override suspend fun fetch(memberId: String): List<LearningAttempt> = client.from(TABLE)
        .select {
            filter { eq("member_id", memberId) }
            order("received_at", Order.ASCENDING)
        }
        .decodeList<LearningAttemptDto>()
        .map { it.toDomain() }

    override suspend fun send(attempt: LearningAttempt) {
        client.from(TABLE).upsert(attempt.toDto()) { ignoreDuplicates = true }
    }

    private companion object {
        const val TABLE = "learning_attempts"
    }
}
