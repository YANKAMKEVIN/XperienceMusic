package com.xperiencemusic.shared.presentation.week

import com.xperiencemusic.shared.domain.model.BandWeek
import com.xperiencemusic.shared.domain.model.SetlistSchedule
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.domain.model.monthStart
import com.xperiencemusic.shared.domain.model.serviceKind
import com.xperiencemusic.shared.domain.usecase.Planning
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
                timing = timingOf(i),
                isSelected = i == index,
                isLastPublished = schedule.isLastPublished(i),
            )
        },
        setlist = schedule.weeks.getOrNull(index)?.toUi(timingOf(index), playingRecordingPath, schedule.isLastPublished(index)),
    )
}

private fun SetlistSchedule.isLastPublished(index: Int) = isCurrentOver && index == currentIndex

private fun WeeklySetlist.toUi(timing: WeekTiming, playingRecordingPath: String?, isLastPublished: Boolean) = WeekSetlistUi(
    serviceDate = setlist.serviceDate,
    timing = timing,
    rehearsalDate = setlist.rehearsalDate,
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
    isLastPublished = isLastPublished,
)

/**
 * The services the signed-in member said "je sers" for, from [today] to the Sunday of the band week
 * (the Friday drops out once it is behind). Confirmed once every month concerned is validated.
 */
fun deriveServingThisWeek(planning: Planning, today: LocalDate): ServingThisWeekUi? {
    val myId = planning.me?.id ?: return null
    val sunday = BandWeek.startingFrom(today).serviceDate
    val dates = planning.signups
        .filter { it.memberId == myId && it.serves && it.serviceDate in today..sunday && it.serviceDate.serviceKind() != null }
        .map { it.serviceDate }
        .distinct()
        .sorted()
    if (dates.isEmpty()) return null
    val validated = planning.months.filter { it.isValidated }.map { it.monthStart }.toSet()
    return ServingThisWeekUi(dates, isConfirmed = dates.all { it.monthStart() in validated })
}
