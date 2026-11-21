package com.xperiencemusic.shared.data.remote.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A song of the list with its recordings, voice tracks and links, as returned by the embedded
 * select. The list leaves the chart out ([chart] is null): the lyrics of two thousand songs are
 * most of the catalogue's weight, and they are fetched apart (see [SongChartDto]).
 */
@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String? = null,
    @SerialName("key_tonic") val keyTonic: Int,
    @SerialName("key_mode") val keyMode: String,
    /** Only in the copies saved on the phone by earlier versions of the app, which held the charts. */
    val chart: String? = null,
    val recordings: List<RecordingDto> = emptyList(),
    @SerialName("voice_tracks") val voiceTracks: List<VoiceTrackDto> = emptyList(),
    @SerialName("song_links") val links: List<SongLinkDto> = emptyList(),
    /** Fetched apart (see [SongThemesDto]): the column only exists once its migration is applied. */
    val themes: List<String> = emptyList(),
) {
    companion object {
        /** Every column the list shows, the chart excepted. */
        const val LIST_COLUMNS =
            "id, title, artist, key_tonic, key_mode, recordings(*), voice_tracks(*), song_links(*)"
    }
}

/**
 * The themes of one song (`songs.themes`, added by the migration `20261121000000_song_themes`).
 * Asked for apart from the list, so the list still loads on a database without the column.
 */
@Serializable
data class SongThemesDto(
    val id: String,
    val themes: List<String>? = null,
) {
    companion object {
        const val COLUMNS = "id, themes"
    }
}

/** The chart of one song, fetched when its page opens or when a search reads the lyrics. */
@Serializable
data class SongChartDto(
    val id: String,
    val chart: String,
) {
    companion object {
        const val COLUMNS = "id, chart"
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
data class VoiceTrackWriteDto(
    val id: String,
    @SerialName("song_id") val songId: String,
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
data class SongLinkWriteDto(
    @SerialName("song_id") val songId: String,
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
