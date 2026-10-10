package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.local.SystemDateProvider
import com.xperiencemusic.shared.data.remote.SupabaseConfig
import com.xperiencemusic.shared.data.remote.createXperienceSupabaseClient
import com.xperiencemusic.shared.data.repository.SupabaseExerciseRepository
import com.xperiencemusic.shared.data.repository.SupabaseSessionRepository
import com.xperiencemusic.shared.data.repository.SupabaseSetlistRepository
import com.xperiencemusic.shared.data.repository.SupabaseSongRepository
import com.xperiencemusic.shared.domain.model.AppEnvironment
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RequestSignInCodeUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SaveSongUseCase
import com.xperiencemusic.shared.domain.usecase.SuggestBandWeekUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import com.xperiencemusic.shared.domain.usecase.VerifySignInCodeUseCase
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorViewModel
import com.xperiencemusic.shared.presentation.signin.SignInViewModel
import com.xperiencemusic.shared.presentation.songeditor.SongEditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Supabase when `local.properties` holds the project keys, otherwise an offline demo. */
val dataModule = module {
    single<DateProvider> { SystemDateProvider() }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    if (SupabaseConfig.isConfigured) {
        single { AppEnvironment(isDemo = false, demoCode = null) }
        single { createXperienceSupabaseClient() }
        single<SessionRepository> { SupabaseSessionRepository(get(), get()) }
        single<SongRepository> { SupabaseSongRepository(get()) }
        single<SetlistRepository> { SupabaseSetlistRepository(get()) }
        single<ExerciseRepository> { SupabaseExerciseRepository(get(), SingerExercises.all) }
    } else {
        single { AppEnvironment(isDemo = true, demoCode = InMemorySessionRepository.DEMO_CODE) }
        single<SessionRepository> { InMemorySessionRepository() }
        single<SongRepository> { InMemorySongRepository(SampleSongs.all(get<DateProvider>().today())) }
        single<SetlistRepository> { InMemorySetlistRepository(SampleSetlist.forWeekOf(get<DateProvider>().today())) }
        single<ExerciseRepository> { InMemoryExerciseRepository(SingerExercises.all) }
    }
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
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::RequestSignInCodeUseCase)
    factoryOf(::VerifySignInCodeUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::RetrySessionUseCase)
    factoryOf(::SaveSongUseCase)
    factoryOf(::SuggestBandWeekUseCase)
}

val presentationModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::WeekViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::SongDetailViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::SetlistEditorViewModel)
    // The song id is optional (null = new song), which the constructor DSL cannot resolve.
    viewModel { params -> SongEditorViewModel(params.getOrNull<String>(), get(), get()) }
}

private var isKoinStarted = false

/** Starts Koin once per process; safe to call from every Activity creation. */
fun initKoin() {
    if (isKoinStarted) return
    startKoin { modules(dataModule, domainModule, presentationModule) }
    isKoinStarted = true
}
