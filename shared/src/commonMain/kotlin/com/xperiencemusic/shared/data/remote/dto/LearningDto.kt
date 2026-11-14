package com.xperiencemusic.shared.data.remote.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LearningAttemptDto(
    val id: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("activity_id") val activityId: String,
    val version: Int,
    val passed: Boolean,
    val day: LocalDate,
    val variant: String = "original",
    val hints: Int = 0,
)

@Serializable
data class ObjectiveRecordingDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("objective_id") val objectiveId: String,
    @SerialName("audio_path") val audioPath: String,
    @SerialName("duration_seconds") val seconds: Int,
    @SerialName("submitted_at") val submittedAt: String? = null,
)

@Serializable
data class ObjectiveRecordingWriteDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("objective_id") val objectiveId: String,
    @SerialName("audio_path") val audioPath: String,
    @SerialName("duration_seconds") val seconds: Int,
)
