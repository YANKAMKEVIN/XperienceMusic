package com.xperiencemusic.shared.presentation.week

data class WeekUiState(
    val isLoading: Boolean = true,
    val setlist: WeekSetlistUi? = null,
    val canPublish: Boolean = false,
)

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
