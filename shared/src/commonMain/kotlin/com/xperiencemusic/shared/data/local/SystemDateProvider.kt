package com.xperiencemusic.shared.data.local

import com.xperiencemusic.shared.domain.repository.DateProvider
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SystemDateProvider : DateProvider {
    @OptIn(ExperimentalTime::class)
    override fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
