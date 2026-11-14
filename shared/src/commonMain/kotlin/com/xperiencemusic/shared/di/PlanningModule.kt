package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.local.FileAnnouncedMonthsStore
import com.xperiencemusic.shared.domain.repository.AnnouncedMonthsStore
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import com.xperiencemusic.shared.domain.usecase.AdjustServiceUseCase
import com.xperiencemusic.shared.domain.usecase.AnswerServiceUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.OpenPlanningMonthUseCase
import com.xperiencemusic.shared.domain.usecase.SyncServiceRemindersUseCase
import com.xperiencemusic.shared.domain.usecase.ValidatePlanningMonthUseCase
import com.xperiencemusic.shared.presentation.planning.AvailabilityViewModel
import com.xperiencemusic.shared.presentation.planning.PlanningViewModel
import com.xperiencemusic.shared.platform.LocalizedReminderScheduler
import com.xperiencemusic.shared.presentation.planning.ServiceDetailViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

/** Who serves when: the monthly planning, the answers and the phone's reminders. */
val planningModule = module {
    single<AnnouncedMonthsStore> { FileAnnouncedMonthsStore(get()) }
    single<ReminderScheduler> { LocalizedReminderScheduler(get()) }
    factory {
        SyncServiceRemindersUseCase(get(), get(), get(), get()) {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }
    factoryOf(::ObservePlanningUseCase)
    factoryOf(::AnswerServiceUseCase)
    factoryOf(::AdjustServiceUseCase)
    factoryOf(::OpenPlanningMonthUseCase)
    factoryOf(::ValidatePlanningMonthUseCase)
    viewModelOf(::PlanningViewModel)
    viewModel { params -> AvailabilityViewModel(params.get<LocalDate>(), get(), get(), get()) }
    viewModel { params -> ServiceDetailViewModel(params.get<LocalDate>(), get(), get(), get(), get()) }
}
