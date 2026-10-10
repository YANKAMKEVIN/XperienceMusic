package com.xperiencemusic.shared.presentation.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
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
) : ViewModel() {
    private val download = MutableStateFlow(DownloadState())

    private data class DownloadState(val isRunning: Boolean = false, val hasFailed: Boolean = false)

    val state: StateFlow<WeekUiState> = combine(
        observeWeeklySetlist(),
        observeCurrentMember(),
        observeSetlistOffline(),
        download,
    ) { setlist, member, offline, download ->
        setlist.toUiState().copy(
            canPublish = member?.role?.can(Permission.PUBLISH_SETLIST) == true,
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
