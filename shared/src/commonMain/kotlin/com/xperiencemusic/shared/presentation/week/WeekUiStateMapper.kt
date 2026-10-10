package com.xperiencemusic.shared.presentation.week

import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.domain.model.WeeklySetlist
import com.xperiencemusic.shared.presentation.common.Formatting

fun WeeklySetlist?.toUiState(): WeekUiState = WeekUiState(
    isLoading = false,
    setlist = this?.let { weekly ->
        WeekSetlistUi(
            rehearsalDate = Formatting.dayAndDate(weekly.setlist.rehearsalDate),
            serviceDate = Formatting.dayAndDate(weekly.setlist.serviceDate),
            message = weekly.setlist.message?.takeIf { it.isNotBlank() },
            publishedBy = weekly.setlist.publishedBy,
            songs = weekly.songs.mapIndexed { index, song ->
                WeekSongUi(
                    id = song.id,
                    position = index + 1,
                    title = song.title,
                    keyLabel = song.key.label(Notation.SOLFEGE),
                    hasReference = song.referenceRecording != null,
                )
            },
        )
    },
)
