package com.xperiencemusic.shared.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDto(val email: String, val role: String)

@Serializable
data class RoleUpdateDto(val role: String)
