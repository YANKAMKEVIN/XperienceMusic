package com.xperiencemusic.shared.presentation.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.BandWeek
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.OfflineAvailability
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.PlaybackTrack
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.Song
import com.xperiencemusic.shared.domain.model.SyncStatus
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import com.xperiencemusic.shared.presentation.common.MemberAvatarUi

class WeekViewModel(
    private val observeSchedule: ObserveSetlistScheduleUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    observeSetlistOffline: ObserveSetlistOfflineUseCase,
    private val downloadSetlist: DownloadSetlistUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val refreshContent: RefreshContentUseCase,
    private val playback: PlaybackController,
    private val getToday: GetTodayUseCase,
    observePlanning: ObservePlanningUseCase,
) : ViewModel() {
    private val download = MutableStateFlow(DownloadState())
    private val isRefreshing = MutableStateFlow(false)
    /** Null follows the current week. */
    private val selectedWeek = MutableStateFlow<LocalDate?>(null)

    private data class DownloadState(val isRunning: Boolean = false, val hasFailed: Boolean = false)

    // A hint above the setlist: without the planning (offline, not loaded yet), the line is simply absent.
    private val serving: Flow<ServingThisWeekUi?> = observePlanning()
        .map<_, ServingThisWeekUi?> { deriveServingThisWeek(it, getToday()) }
        .onStart { emit(null) }
        .catch { emit(null) }

    private data class SyncState(val status: SyncStatus, val isRefreshing: Boolean, val serving: ServingThisWeekUi?)

    private val sync = combine(observeSyncStatus(), isRefreshing, serving, ::SyncState)

    // Null until the phone has a copy of the setlists: the schedule publishes nothing before, and
    // the screen would wait on it forever when the first load fails.
    private val week: Flow<WeekUiState?> = combine(observeSchedule(), selectedWeek, playback.state) { schedule, selected, playing ->
        deriveWeekState(schedule, selected, playing.track?.audioPath?.takeIf { playing.isPlaying || playing.isLoading })
    }.map<WeekUiState, WeekUiState?> { it }.onStart { emit(null) }

    val state: StateFlow<WeekUiState> = combine(
        week,
        observeCurrentMember(),
        // Like the week, it waits on the schedule: nothing to download until there is a copy.
        observeSetlistOffline().onStart { emit(OfflineAvailability(available = 0, total = 0)) },
        download,
        sync,
    ) { week, member, offline, download, sync ->
        val hasFailed = sync.status == SyncStatus.FAILED
        (week ?: WeekUiState(isLoading = !hasFailed, hasLoadFailed = hasFailed)).copy(
            canPublish = member?.role?.can(Permission.PUBLISH_SETLIST) == true,
            me = member?.let { MemberAvatarUi(it.profile.displayName, it.avatar) },
            nextRehearsal = getToday().let { today ->
                val rehearsal = BandWeek.nextRehearsal(today)
                RehearsalCountdownUi(today.daysUntil(rehearsal), rehearsal)
            },
            isRefreshing = sync.isRefreshing,
            serving = sync.serving,
            isOffline = week != null && hasFailed,
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
            WeekEvent.PlayAllClicked -> playAll()
        }
    }

    /** The references of the week on screen, in the setlist's order; songs without one are skipped. */
    private fun playAll() {
        val songIds = state.value.setlist?.songs?.map { it.id } ?: return
        viewModelScope.launch {
            val songs = observeSchedule().first().weeks.flatMap { it.songs }.associateBy { it.id }
            playback.playAll(songIds.mapNotNull { songs[it]?.referenceTrack() })
        }
    }

    private fun playReference(songId: String) {
        viewModelScope.launch {
            val song = observeSchedule().firstSong(songId) ?: return@launch
            playback.playOrToggle(song.referenceTrack() ?: return@launch)
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

private fun Song.referenceTrack(): PlaybackTrack? =
    referenceRecording?.let { PlaybackTrack(it.id, it.title, title, it.audioPath) }
