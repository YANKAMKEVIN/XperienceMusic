package com.xperiencemusic.shared.presentation.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class WeekViewModel(
    private val observeSchedule: ObserveSetlistScheduleUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSetlistOffline: ObserveSetlistOfflineUseCase,
    private val downloadSetlist: DownloadSetlistUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val refreshContent: RefreshContentUseCase,
    private val playback: PlaybackController,
) : ViewModel() {
    private val download = MutableStateFlow(DownloadState())
    private val isRefreshing = MutableStateFlow(false)
    /** Null follows the current week. */
    private val selectedWeek = MutableStateFlow<LocalDate?>(null)

    private data class DownloadState(val isRunning: Boolean = false, val hasFailed: Boolean = false)

    private val sync = combine(observeSyncStatus(), isRefreshing) { status, refreshing -> status to refreshing }

    private val week = combine(observeSchedule(), selectedWeek, playback.state) { schedule, selected, playing ->
        deriveWeekState(schedule, selected, playing.track?.audioPath?.takeIf { playing.isPlaying || playing.isLoading })
    }

    val state: StateFlow<WeekUiState> = combine(
        week,
        observeCurrentMember(),
        observeSetlistOffline(),
        download,
        sync,
    ) { week, member, offline, download, (syncStatus, refreshing) ->
        week.copy(
            canPublish = member?.role?.can(Permission.PUBLISH_SETLIST) == true,
            isRefreshing = refreshing,
            isOffline = syncStatus == SyncStatus.FAILED,
            offline = offline.takeIf { it.total > 0 }?.let {
                WeekOfflineUi(
                    available = it.available,
                    total = it.total,
                    isComplete = it.isComplete,
                    isDownloading = download.isRunning,
                    hasError = download.hasFailed,
                )
            },
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUiState())

    fun onEvent(event: WeekEvent) {
        when (event) {
            WeekEvent.DownloadClicked -> startDownload()
            WeekEvent.RefreshPulled -> refresh()
            is WeekEvent.WeekSelected -> selectedWeek.value = event.serviceDate
            is WeekEvent.PlayReferenceClicked -> playReference(event.songId)
        }
    }

    private fun playReference(songId: String) {
        viewModelScope.launch {
            val song = observeSchedule().firstSong(songId) ?: return@launch
            val reference = song.referenceRecording ?: return@launch
            playback.playOrToggle(PlaybackTrack(reference.id, reference.title, song.title, reference.audioPath))
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch {
            // A failure shows as the offline banner, through the sync status.
            refreshContent()
            isRefreshing.value = false
        }
    }

    private fun startDownload() {
        if (download.value.isRunning) return
        download.value = DownloadState(isRunning = true)
        viewModelScope.launch {
            val failed = downloadSetlist() is DomainResult.Failure
            download.value = DownloadState(isRunning = false, hasFailed = failed)
        }
    }
}

private suspend fun Flow<SetlistSchedule>.firstSong(id: String) =
    first().weeks.firstNotNullOfOrNull { weekly -> weekly.songs.firstOrNull { it.id == id } }
