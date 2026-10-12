package com.xperiencemusic.shared.presentation.week

import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.presentation.common.Formatting
import com.xperiencemusic.shared.presentation.common.shortLabel
import kotlinx.datetime.LocalDate

/**
 * The week to show is [selected] when it is still kept, the current week otherwise.
 * [playingRecordingPath] marks the song whose reference is playing.
 */
fun deriveWeekState(schedule: SetlistSchedule, selected: LocalDate?, playingRecordingPath: String?): WeekUiState {
    val index = schedule.weeks.indexOfFirst { it.setlist.serviceDate == selected }
        .takeIf { it >= 0 } ?: schedule.currentIndex
    fun timingOf(i: Int) = when {
        i < schedule.currentIndex -> WeekTiming.PAST
        i == schedule.currentIndex -> WeekTiming.CURRENT
        else -> WeekTiming.AHEAD
    }
    return WeekUiState(
        isLoading = false,
        weeks = schedule.weeks.mapIndexed { i, weekly ->
            WeekTabUi(
                serviceDate = weekly.setlist.serviceDate,
                label = Formatting.shortDayAndDate(weekly.setlist.serviceDate),
                timing = timingOf(i),
                isSelected = i == index,
            )
        },
        setlist = schedule.weeks.getOrNull(index)?.toUi(timingOf(index), playingRecordingPath),
    )
}

private fun WeeklySetlist.toUi(timing: WeekTiming, playingRecordingPath: String?) = WeekSetlistUi(
    serviceDate = setlist.serviceDate,
    timing = timing,
    rangeLabel = Formatting.range(setlist.rehearsalDate, setlist.serviceDate),
    rehearsalDate = Formatting.dayAndDate(setlist.rehearsalDate),
    serviceDateLabel = Formatting.dayAndDate(setlist.serviceDate),
    message = setlist.message?.takeIf { it.isNotBlank() },
    publishedBy = setlist.publishedBy,
    songs = songs.mapIndexed { index, song ->
        val reference = song.referenceRecording
        WeekSongUi(
            id = song.id,
            position = index + 1,
            title = song.title,
            keyLabel = song.key.shortLabel(),
            hasReference = reference != null,
            isPlaying = reference != null && reference.audioPath == playingRecordingPath,
        )
    },
)
