package com.xperiencemusic.shared.presentation.song

import com.xperiencemusic.music.chart.ChartLine
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.VoicePart

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
    val voiceTracks: List<VoicePart> = emptyList(),
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
}
