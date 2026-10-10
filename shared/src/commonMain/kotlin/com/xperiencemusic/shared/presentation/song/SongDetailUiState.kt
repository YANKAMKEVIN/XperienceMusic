package com.xperiencemusic.shared.presentation.song

import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.common.PlaybackStatus

data class SongDetailUiState(
    val isLoading: Boolean = true,
    val isMissing: Boolean = false,
    val title: String = "",
    val artist: String? = null,
    val keyLabel: String = "",
    /** Set only while the song is transposed away from its original key. */
    val originalKeyLabel: String? = null,
    val notation: Notation = Notation.NASHVILLE,
    val chart: List<ChartLine> = emptyList(),
    val recordings: List<RecordingUi> = emptyList(),
    val maxRecordings: Int = 0,
    val canManageRecordings: Boolean = false,
    val canEditSong: Boolean = false,
    val voiceTracks: List<VoiceTrackUi> = emptyList(),
    val links: List<LinkUi> = emptyList(),
    val pendingDeletion: RecordingUi? = null,
    val message: SongMessage? = null,
)

data class RecordingUi(
    val id: String,
    val title: String,
    val date: String,
    val duration: String,
    val isReference: Boolean,
    val playback: PlaybackStatus = PlaybackStatus.IDLE,
    val isAvailableOffline: Boolean = false,
)

data class VoiceTrackUi(
    val id: String,
    val voicePart: VoicePart,
    val playback: PlaybackStatus = PlaybackStatus.IDLE,
)

data class LinkUi(val label: String, val url: String)

enum class SongMessage { FORBIDDEN, GENERIC }

sealed interface SongDetailEvent {
    data object TransposeUpClicked : SongDetailEvent
    data object TransposeDownClicked : SongDetailEvent
    data object TransposeResetClicked : SongDetailEvent
    data class NotationSelected(val notation: Notation) : SongDetailEvent
    data class SetReferenceClicked(val recordingId: String) : SongDetailEvent
    data class DeleteClicked(val recordingId: String) : SongDetailEvent
    data object DeleteConfirmed : SongDetailEvent
    data object DeleteDismissed : SongDetailEvent
    data object MessageShown : SongDetailEvent
    data class PlayRecordingClicked(val recordingId: String) : SongDetailEvent
    /** [title] is the voice name as displayed, shown again in the player. */
    data class PlayVoiceTrackClicked(val trackId: String, val title: String) : SongDetailEvent
}
