package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.local.SystemDateProvider
import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.data.repository.InMemorySessionRepository
import com.xperiencemusic.shared.data.repository.InMemorySetlistRepository
import com.xperiencemusic.shared.data.repository.InMemorySongRepository
import com.xperiencemusic.shared.data.seed.SampleSetlist
import com.xperiencemusic.shared.data.seed.SampleSongs
import com.xperiencemusic.shared.data.seed.SingerExercises
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.domain.usecase.AddRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.CompleteOnboardingUseCase
import com.xperiencemusic.shared.domain.usecase.DeleteRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveWeeklySetlistUseCase
import com.xperiencemusic.shared.domain.usecase.PublishSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.domain.usecase.SetReferenceRecordingUseCase
import com.xperiencemusic.shared.presentation.app.AppViewModel
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.presentation.onboarding.OnboardingViewModel
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import com.xperiencemusic.shared.presentation.song.SongDetailViewModel
import com.xperiencemusic.shared.presentation.week.WeekViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** In-memory data with demo content; swapped for Supabase-backed repositories later. */
val dataModule = module {
    single<DateProvider> { SystemDateProvider() }
    single<SessionRepository> { InMemorySessionRepository() }
    single<SongRepository> { InMemorySongRepository(SampleSongs.all(get<DateProvider>().today())) }
    single<SetlistRepository> { InMemorySetlistRepository(SampleSetlist.forWeekOf(get<DateProvider>().today())) }
    single<ExerciseRepository> { InMemoryExerciseRepository(SingerExercises.all) }
}

val domainModule = module {
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::ObserveCurrentMemberUseCase)
    factoryOf(::SearchSongsUseCase)
    factoryOf(::ObserveSongUseCase)
    factoryOf(::AddRecordingUseCase)
    factoryOf(::DeleteRecordingUseCase)
    factoryOf(::SetReferenceRecordingUseCase)
    factoryOf(::ObserveWeeklySetlistUseCase)
    factoryOf(::PublishSetlistUseCase)
    factoryOf(::ObserveSingerPathUseCase)
}

val presentationModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::WeekViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::SongDetailViewModel)
}

private var isKoinStarted = false

/** Starts Koin once per process; safe to call from every Activity creation. */
fun initKoin() {
    if (isKoinStarted) return
    startKoin { modules(dataModule, domainModule, presentationModule) }
    isKoinStarted = true
}
