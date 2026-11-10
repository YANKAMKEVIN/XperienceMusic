package com.xperiencemusic.shared.presentation.rehearsal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.presentation.common.Formatting
import com.xperiencemusic.shared.presentation.common.shortLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

class RehearsalUploadViewModel(
    serviceDate: LocalDate,
    observeSchedule: ObserveSetlistScheduleUseCase,
) : ViewModel() {
    val state: StateFlow<RehearsalUploadUiState> = observeSchedule()
        .map { deriveRehearsalUpload(it, serviceDate) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RehearsalUploadUiState())
}

/** Pure: the songs of the week of [serviceDate] and what the band works from for each. */
internal fun deriveRehearsalUpload(schedule: SetlistSchedule, serviceDate: LocalDate): RehearsalUploadUiState {
    val week = schedule.weeks.firstOrNull { it.setlist.serviceDate == serviceDate }
        ?: return RehearsalUploadUiState(isLoading = false)
    val rehearsal = week.setlist.rehearsalDate
    return RehearsalUploadUiState(
        isLoading = false,
        rehearsalDate = rehearsal,
        dateLabel = Formatting.dayAndDate(rehearsal),
        songs = week.songs.map { song ->
            RehearsalSongUi(
                id = song.id,
                title = song.title,
                keyLabel = song.key.shortLabel(),
                referenceTitle = song.referenceRecording?.title,
                voiceParts = song.voiceTracks.map { it.voicePart }.sortedBy { it.ordinal },
                hasNewTake = song.recordings.any { it.recordedOn >= rehearsal },
            )
        },
    )
}
