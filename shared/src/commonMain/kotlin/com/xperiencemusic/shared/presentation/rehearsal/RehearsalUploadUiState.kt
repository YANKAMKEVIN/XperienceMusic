package com.xperiencemusic.shared.presentation.rehearsal

import com.xperiencemusic.shared.domain.model.VoicePart
import kotlinx.datetime.LocalDate

data class RehearsalUploadUiState(
    val isLoading: Boolean = true,
    val rehearsalDate: LocalDate? = null,
    val songs: List<RehearsalSongUi> = emptyList(),
)

/** A song without a new take is not a task left: the band simply keeps working from its reference. */
data class RehearsalSongUi(
    val id: String,
    val title: String,
    val keyLabel: String,
    val referenceTitle: String?,
    val voiceParts: List<VoicePart>,
    val hasNewTake: Boolean,
)
