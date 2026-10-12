package com.xperiencemusic.shared.presentation.week

import kotlinx.datetime.LocalDate

data class WeekUiState(
    val isLoading: Boolean = true,
    /** Every kept week, in date order; empty when nothing was ever published. */
    val weeks: List<WeekTabUi> = emptyList(),
    /** The week being looked at. */
    val setlist: WeekSetlistUi? = null,
    val canPublish: Boolean = false,
    /** Pull-to-refresh under way. */
    val isRefreshing: Boolean = false,
    /** The server could not be reached: what is shown is the copy on the phone. */
    val isOffline: Boolean = false,
    /** Null when the coming weeks have nothing to download. */
    val offline: WeekOfflineUi? = null,
    /** The next rehearsal, counted down while no setlist is published. */
    val nextRehearsal: RehearsalCountdownUi? = null,
) {
    /** Admins edit a week whose service is still ahead; past weeks stay as they were. */
    val canEditSelected: Boolean get() = canPublish && setlist != null && setlist.timing != WeekTiming.PAST
}

enum class WeekTiming { PAST, CURRENT, AHEAD }

data class RehearsalCountdownUi(
    /** 0 on the day itself. */
    val daysLeft: Int,
    /** "mardi 13 oct." */
    val dateLabel: String,
) {
    /** How much of the week before it is over: the ring of the countdown. */
    val fraction: Float get() = 1f - daysLeft / 7f
}

data class WeekTabUi(
    val serviceDate: LocalDate,
    /** "dim. 19 oct." */
    val label: String,
    val timing: WeekTiming,
    val isSelected: Boolean,
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
    data object RefreshPulled : WeekEvent
    data class WeekSelected(val serviceDate: LocalDate) : WeekEvent
    data class PlayReferenceClicked(val songId: String) : WeekEvent
}

data class WeekSetlistUi(
    val serviceDate: LocalDate,
    val timing: WeekTiming,
    /** "13 – 18 octobre" */
    val rangeLabel: String,
    val rehearsalDate: String,
    val serviceDateLabel: String,
    val message: String?,
    val publishedBy: String,
    val songs: List<WeekSongUi>,
)

data class WeekSongUi(
    val id: String,
    val position: Int,
    val title: String,
    /** "Sol" */
    val keyLabel: String,
    val hasReference: Boolean,
    val isPlaying: Boolean = false,
)
