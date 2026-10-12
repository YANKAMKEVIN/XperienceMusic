package com.xperiencemusic.shared.data.remote.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PracticeDayDto(
    @SerialName("member_id") val memberId: String,
    val day: LocalDate,
)
