package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.BandWeek
import com.xperiencemusic.shared.domain.repository.DateProvider

class SuggestBandWeekUseCase(private val dates: DateProvider) {
    operator fun invoke(): BandWeek = BandWeek.startingFrom(dates.today())
}
