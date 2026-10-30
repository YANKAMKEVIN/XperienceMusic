package com.xperiencemusic.shared.data.learning

import com.xperiencemusic.shared.domain.learning.LearningAttempt
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class AttemptDto(
    val id: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("activity_id") val activityId: String,
    val version: Int,
    val passed: Boolean,
    val day: LocalDate,
    val variant: String = "original",
    val hints: Int = 0,
) {
    fun domain() = LearningAttempt(id, memberId, activityId, version, passed, day, variant, hints)
}

class SupabaseLearningRemote(private val client: SupabaseClient) : LearningRemote {
    override suspend fun fetch(memberId: String): List<LearningAttempt> = client.from("learning_attempts")
        .select { filter { eq("member_id", memberId) }; order("received_at", Order.ASCENDING) }.decodeList<AttemptDto>().map { it.domain() }

    override suspend fun send(attempt: LearningAttempt) {
        client.from("learning_attempts").upsert(
            AttemptDto(attempt.id, attempt.memberId, attempt.activityId, attempt.version, attempt.passed, attempt.day, attempt.variant, attempt.hints),
        ) { ignoreDuplicates = true }
    }
}
