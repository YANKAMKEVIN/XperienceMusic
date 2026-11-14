package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.domain.usecase.DeleteSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.PublishSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.SuggestBandWeekUseCase
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorViewModel
import com.xperiencemusic.shared.presentation.week.WeekViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The week's setlist, its editor and the offline copy of its songs. */
val setlistsModule = module {
    factoryOf(::ObserveSetlistScheduleUseCase)
    factoryOf(::PublishSetlistUseCase)
    factoryOf(::DeleteSetlistUseCase)
    factoryOf(::SuggestBandWeekUseCase)
    factoryOf(::GetTodayUseCase)
    factoryOf(::ObserveSetlistOfflineUseCase)
    factoryOf(::DownloadSetlistUseCase)
    factoryOf(::ObserveSyncStatusUseCase)
    factoryOf(::RefreshContentUseCase)
    viewModelOf(::WeekViewModel)
    // The service date is optional (null = the coming week), which the constructor DSL cannot resolve.
    viewModel { params -> SetlistEditorViewModel(get(), get(), get(), get(), get(), params.getOrNull()) }
}
