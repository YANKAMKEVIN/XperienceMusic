package com.xperiencemusic.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceMonthDto(
    val month: String,
    @SerialName("answer_by") val answerBy: String? = null,
    @SerialName("validated_at") val validatedAt: String? = null,
)

@Serializable
data class ServiceMonthOpenDto(
    val month: String,
    @SerialName("answer_by") val answerBy: String?,
)

@Serializable
data class ServiceMonthValidateDto(@SerialName("validated_at") val validatedAt: String)

@Serializable
data class ServiceSignupDto(
    @SerialName("service_date") val serviceDate: String,
    @SerialName("member_id") val memberId: String,
    val serves: Boolean,
)

@Serializable
data class ServiceSignupUpsertDto(
    @SerialName("service_date") val serviceDate: String,
    @SerialName("member_id") val memberId: String,
    val serves: Boolean,
    @SerialName("updated_at") val updatedAt: String,
)
