package com.xperiencemusic.shared.data.remote.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A song with its recordings, voice tracks and links, as returned by the embedded select. */
@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String? = null,
    @SerialName("key_tonic") val keyTonic: Int,
    @SerialName("key_mode") val keyMode: String,
    val chart: String,
    val recordings: List<RecordingDto> = emptyList(),
    @SerialName("voice_tracks") val voiceTracks: List<VoiceTrackDto> = emptyList(),
    @SerialName("song_links") val links: List<SongLinkDto> = emptyList(),
) {
    companion object {
        const val COLUMNS = "*, recordings(*), voice_tracks(*), song_links(*)"
    }
}

@Serializable
data class RecordingDto(
    val id: String,
    val title: String,
    @SerialName("recorded_on") val recordedOn: LocalDate,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("is_reference") val isReference: Boolean,
)

@Serializable
data class VoiceTrackDto(
    val id: String,
    @SerialName("voice_part") val voicePart: String,
    @SerialName("storage_path") val storagePath: String,
)

@Serializable
data class SongLinkDto(
    val label: String,
    val url: String,
    val position: Int = 0,
)

@Serializable
data class SongWriteDto(
    val title: String,
    val artist: String?,
    @SerialName("key_tonic") val keyTonic: Int,
    @SerialName("key_mode") val keyMode: String,
    val chart: String,
)

@Serializable
data class IdDto(val id: String)
