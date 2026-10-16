package com.xperiencemusic.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GrowerDto(
    @SerialName("display_name") val displayName: String? = null,
    val instrument: String? = null,
    @SerialName("voice_part") val voicePart: String? = null,
)

@Serializable
data class LevelRequestDto(
    val id: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("target_level") val targetLevel: String,
    val status: String,
    val feedback: String? = null,
    @SerialName("submitted_at") val submittedAt: String,
    val profiles: GrowerDto? = null,
) {
    companion object {
        const val COLUMNS = "*, profiles(display_name, instrument, voice_part)"
    }
}

@Serializable
data class ObjectiveCheckDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("objective_id") val objectiveId: String,
    val status: String,
    val feedback: String? = null,
    @SerialName("submitted_at") val submittedAt: String,
    val profiles: GrowerDto? = null,
) {
    companion object {
        const val COLUMNS = "*, profiles(display_name, instrument, voice_part)"
    }
}

/** Asking (again): back to SUBMITTED, the earlier answer cleared. */
@Serializable
data class LevelRequestUpsertDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("target_level") val targetLevel: String,
    @SerialName("submitted_at") val submittedAt: String,
    val status: String = "SUBMITTED",
    val feedback: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
)

@Serializable
data class ObjectiveCheckUpsertDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("objective_id") val objectiveId: String,
    @SerialName("submitted_at") val submittedAt: String,
    val status: String = "SUBMITTED",
    val feedback: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
)

@Serializable
data class GrowthReviewDto(
    val status: String,
    val feedback: String?,
    @SerialName("reviewed_at") val reviewedAt: String,
)
