package com.xperiencemusic.shared.domain.model

import kotlinx.datetime.LocalDate

/** The songs of the week: rehearsed on Tuesday, performed on Sunday. */
data class Setlist(
    val id: String,
    val rehearsalDate: LocalDate,
    val serviceDate: LocalDate,
    val songIds: List<String>,
    val message: String?,
    val publishedBy: String,
)

/** A setlist with its songs resolved, ready to be shown. */
data class WeeklySetlist(
    val setlist: Setlist,
    val songs: List<Song>,
)
