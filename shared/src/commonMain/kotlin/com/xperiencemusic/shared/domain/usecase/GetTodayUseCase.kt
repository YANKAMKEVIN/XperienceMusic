package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.repository.DateProvider
import kotlinx.datetime.LocalDate

class GetTodayUseCase(private val dates: DateProvider) {
    operator fun invoke(): LocalDate = dates.today()
}
