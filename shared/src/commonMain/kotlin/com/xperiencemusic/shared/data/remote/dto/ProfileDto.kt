package com.xperiencemusic.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
    val instrument: String? = null,
    @SerialName("voice_part") val voicePart: String? = null,
    val level: String? = null,
)

@Serializable
data class ProfileUpdateDto(
    @SerialName("display_name") val displayName: String,
    val instrument: String,
    @SerialName("voice_part") val voicePart: String?,
    val level: String,
)
