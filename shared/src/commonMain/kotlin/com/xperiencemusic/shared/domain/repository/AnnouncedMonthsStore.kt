package com.xperiencemusic.shared.domain.repository

import kotlinx.datetime.LocalDate

/** The validated months the member was already told about, kept on the phone. */
interface AnnouncedMonthsStore {
    fun load(): Set<LocalDate>

    fun save(months: Set<LocalDate>)
}
