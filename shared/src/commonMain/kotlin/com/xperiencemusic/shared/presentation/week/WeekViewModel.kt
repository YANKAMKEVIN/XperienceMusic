package com.xperiencemusic.shared.presentation.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveWeeklySetlistUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeekViewModel(
    observeWeeklySetlist: ObserveWeeklySetlistUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSetlistOffline: ObserveSetlistOfflineUseCase,
    private val downloadSetlist: DownloadSetlistUseCase,
    private val getToday: GetTodayUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val refreshContent: RefreshContentUseCase,
) : ViewModel() {
    private val download = MutableStateFlow(DownloadState())
    private val isRefreshing = MutableStateFlow(false)

    private data class DownloadState(val isRunning: Boolean = false, val hasFailed: Boolean = false)

    private val sync = combine(observeSyncStatus(), isRefreshing) { status, refreshing -> status to refreshing }

    val state: StateFlow<WeekUiState> = combine(
        observeWeeklySetlist(),
        observeCurrentMember(),
        observeSetlistOffline(),
        download,
        sync,
    ) { setlist, member, offline, download, (syncStatus, refreshing) ->
        setlist.toUiState().copy(
            canPublish = member?.role?.can(Permission.PUBLISH_SETLIST) == true,
            isSetlistUpcoming = setlist?.setlist?.serviceDate?.let { it >= getToday() } == true,
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
