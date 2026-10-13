package com.xperiencemusic.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceSubmissionDto(
    val id: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("audio_path") val audioPath: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val status: String,
    val feedback: String? = null,
    @SerialName("submitted_at") val submittedAt: String,
    val profiles: SubmitterDto? = null,
    val songs: SubmittedSongDto? = null,
) {
    companion object {
        const val COLUMNS = "*, profiles(display_name, voice_part), songs(title)"
    }
}

@Serializable
data class SubmitterDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("voice_part") val voicePart: String? = null,
)

@Serializable
data class SubmittedSongDto(val title: String)

/** Sending (or sending again): back to SUBMITTED, the earlier verdict cleared. */
@Serializable
data class VoiceSubmissionUpsertDto(
    @SerialName("member_id") val memberId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("audio_path") val audioPath: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("submitted_at") val submittedAt: String,
    val status: String = "SUBMITTED",
    val feedback: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
)

@Serializable
data class VoiceReviewDto(
    val status: String,
    val feedback: String?,
    @SerialName("reviewed_at") val reviewedAt: String,
)

@Serializable
data class AudioPathDto(@SerialName("audio_path") val audioPath: String)
