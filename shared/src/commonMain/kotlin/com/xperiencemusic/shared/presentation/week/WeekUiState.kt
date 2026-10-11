package com.xperiencemusic.shared.presentation.week

data class WeekUiState(
    val isLoading: Boolean = true,
    val setlist: WeekSetlistUi? = null,
    val canPublish: Boolean = false,
    /** The published setlist is for a service still ahead: admins edit it rather than start anew. */
    val isSetlistUpcoming: Boolean = false,
    /** Null when the week has nothing to download. */
    val offline: WeekOfflineUi? = null,
)

data class WeekOfflineUi(
    val available: Int,
    val total: Int,
    val isComplete: Boolean,
    val isDownloading: Boolean,
    val hasError: Boolean,
)

sealed interface WeekEvent {
    data object DownloadClicked : WeekEvent
}

data class WeekSetlistUi(
    val rehearsalDate: String,
    val serviceDate: String,
    val message: String?,
    val publishedBy: String,
    val songs: List<WeekSongUi>,
)

data class WeekSongUi(
    val id: String,
    val position: Int,
    val title: String,
    val keyLabel: String,
    val hasReference: Boolean,
)
