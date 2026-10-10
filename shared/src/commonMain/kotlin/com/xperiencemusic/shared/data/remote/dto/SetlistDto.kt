package com.xperiencemusic.shared.data.remote.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetlistDto(
    val id: String,
    @SerialName("rehearsal_date") val rehearsalDate: LocalDate,
    @SerialName("service_date") val serviceDate: LocalDate,
    val message: String? = null,
    @SerialName("published_by_name") val publishedByName: String,
    @SerialName("setlist_songs") val songs: List<SetlistSongDto> = emptyList(),
) {
    companion object {
        const val COLUMNS = "*, setlist_songs(position, song_id)"
    }
}

@Serializable
data class SetlistSongDto(
    val position: Int,
    @SerialName("song_id") val songId: String,
)
