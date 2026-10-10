package com.xperiencemusic.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseProgressDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("exercise_id") val exerciseId: String,
    val status: String,
)
