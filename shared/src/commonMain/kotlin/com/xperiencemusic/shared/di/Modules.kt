package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.presentation.voice.VoiceRecordViewModel
import com.xperiencemusic.shared.presentation.voice.VoiceReviewViewModel
import com.xperiencemusic.shared.data.repository.InMemoryVoiceSubmissionRepository
import com.xperiencemusic.shared.data.repository.SupabaseVoiceSubmissionRepository
import com.xperiencemusic.shared.data.seed.SampleSubmissions
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import com.xperiencemusic.shared.domain.usecase.ReviewVoiceUseCase
import com.xperiencemusic.shared.domain.usecase.SendVoiceUseCase
import com.xperiencemusic.shared.domain.usecase.VoiceRecorder
import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.data.local.DeviceDataStore
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.local.SystemDateProvider
import com.xperiencemusic.shared.data.remote.SupabaseConfig
import com.xperiencemusic.shared.data.remote.createXperienceSupabaseClient
import com.xperiencemusic.shared.data.repository.DemoAudioCache
import com.xperiencemusic.shared.data.repository.DemoAudioUrlProvider
import com.xperiencemusic.shared.data.repository.FileAudioCache
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.repository.SupabaseMembersRepository
import com.xperiencemusic.shared.data.repository.OfflineFirstAudioUrlProvider
import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.data.repository.SupabaseAudioUrlProvider
import com.xperiencemusic.shared.data.repository.SupabaseAudioStorage
import com.xperiencemusic.shared.data.repository.SupabaseExerciseRepository
import com.xperiencemusic.shared.data.repository.SupabaseSessionRepository
import com.xperiencemusic.shared.data.repository.SupabaseSetlistRepository
import com.xperiencemusic.shared.data.repository.SupabaseSongRepository
import com.xperiencemusic.shared.domain.model.AppEnvironment
import com.xperiencemusic.shared.domain.usecase.ObserveSessionUseCase
import com.xperiencemusic.shared.domain.usecase.RequestSignInCodeUseCase
import com.xperiencemusic.shared.domain.usecase.RetrySessionUseCase
import com.xperiencemusic.shared.domain.usecase.SaveSongUseCase
import com.xperiencemusic.shared.domain.usecase.CancelInvitationUseCase
import com.xperiencemusic.shared.domain.usecase.ChangeMemberRoleUseCase
import com.xperiencemusic.shared.domain.usecase.CompleteExerciseUseCase
import com.xperiencemusic.shared.domain.usecase.InviteMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveBandUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSyncStatusUseCase
import com.xperiencemusic.shared.domain.usecase.RefreshContentUseCase
import com.xperiencemusic.shared.domain.usecase.RemoveMemberUseCase
import com.xperiencemusic.shared.domain.usecase.DeleteSongUseCase
import com.xperiencemusic.shared.domain.usecase.DownloadSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.PlayReferenceNoteUseCase
import com.xperiencemusic.shared.domain.usecase.TrackSingingUseCase
import com.xperiencemusic.shared.domain.usecase.GetTodayUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveOfflineAudioUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistOfflineUseCase
import com.xperiencemusic.shared.domain.usecase.SuggestBandWeekUseCase
import com.xperiencemusic.shared.domain.usecase.UploadRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.SignOutUseCase
import com.xperiencemusic.shared.domain.usecase.VerifySignInCodeUseCase
import com.xperiencemusic.shared.presentation.exercise.ExerciseViewModel
import com.xperiencemusic.shared.presentation.members.MembersViewModel
import com.xperiencemusic.shared.presentation.player.MiniPlayerViewModel
import com.xperiencemusic.shared.presentation.stage.StageSettingsStore
import com.xperiencemusic.shared.presentation.stage.StageViewModel
import com.xperiencemusic.shared.presentation.recordingform.RecordingFormViewModel
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
import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.domain.usecase.AddRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.CompleteOnboardingUseCase
import com.xperiencemusic.shared.domain.usecase.DeleteRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSongUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import com.xperiencemusic.shared.domain.usecase.DeleteSetlistUseCase
import com.xperiencemusic.shared.signInLinkInbox
import com.xperiencemusic.shared.domain.usecase.PublishSetlistUseCase
import com.xperiencemusic.shared.domain.usecase.SearchSongsUseCase
import com.xperiencemusic.shared.domain.usecase.SetReferenceRecordingUseCase
import com.xperiencemusic.shared.presentation.app.AppViewModel
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.presentation.onboarding.OnboardingViewModel
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import com.xperiencemusic.shared.presentation.song.SongDetailViewModel
import com.xperiencemusic.shared.presentation.week.WeekViewModel
import com.xperiencemusic.shared.platform.platformModule
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import com.xperiencemusic.shared.data.local.FileAnnouncedMonthsStore
import com.xperiencemusic.shared.domain.repository.AnnouncedMonthsStore
import com.xperiencemusic.shared.presentation.reminders.RemindersViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import com.xperiencemusic.shared.presentation.planning.AvailabilityViewModel
import com.xperiencemusic.shared.presentation.planning.PlanningViewModel
import com.xperiencemusic.shared.presentation.planning.ServiceDetailViewModel
import com.xperiencemusic.shared.data.repository.InMemoryPlanningRepository
import com.xperiencemusic.shared.data.repository.SupabasePlanningRepository
import com.xperiencemusic.shared.data.seed.SamplePlanning
import com.xperiencemusic.shared.domain.repository.PlanningRepository
import com.xperiencemusic.shared.domain.usecase.AdjustServiceUseCase
import com.xperiencemusic.shared.domain.usecase.AnswerServiceUseCase
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.OpenPlanningMonthUseCase
import com.xperiencemusic.shared.domain.usecase.ValidatePlanningMonthUseCase
import io.ktor.client.HttpClient
import com.xperiencemusic.shared.data.remote.AccordsAppPageSource
import com.xperiencemusic.shared.domain.repository.SongPageSource
import com.xperiencemusic.shared.domain.usecase.ImportSongFromLinkUseCase

/** Supabase when `local.properties` holds the project keys, otherwise an offline demo. */
val dataModule = module {
    single { HttpClient() }
    single<SongPageSource> { AccordsAppPageSource(get()) }
    single<DateProvider> { SystemDateProvider() }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    // Drives the platform players, which expect the main thread.
    single { PlaybackController(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Main)) }
    single { SnapshotStore(get()) }
    single<LocalDataStore> { DeviceDataStore(get(), get(), get(), get()) }
    single<AnnouncedMonthsStore> { FileAnnouncedMonthsStore(get()) }
    if (SupabaseConfig.isConfigured) {
        single { AppEnvironment(isDemo = false, demoCode = null) }
        single { createXperienceSupabaseClient() }
        single<SessionRepository> { SupabaseSessionRepository(get(), get(), get()) }
        single<SongRepository> { SupabaseSongRepository(get(), get()) }
        single<SetlistRepository> { SupabaseSetlistRepository(get(), get()) }
        single<ExerciseRepository> { SupabaseExerciseRepository(get(), SingerExercises.all, get()) }
        single<AudioStorage> { SupabaseAudioStorage(get()) }
        single<AudioCache> { FileAudioCache(get(), get()) }
        single<MembersRepository> { SupabaseMembersRepository(get()) }
        single<AudioUrlProvider> { OfflineFirstAudioUrlProvider(get(), SupabaseAudioUrlProvider(get())) }
        single<VoiceSubmissionRepository> { SupabaseVoiceSubmissionRepository(get()) }
        single<PlanningRepository> { SupabasePlanningRepository(get()) }
    } else {
        single { AppEnvironment(isDemo = true, demoCode = InMemorySessionRepository.DEMO_CODE) }
        single<SessionRepository> { InMemorySessionRepository() }
        single<PlanningRepository> {
            val today = get<DateProvider>().today()
            InMemoryPlanningRepository(SamplePlanning.months(today), SamplePlanning.signups(today))
        }
        single<SongRepository> { InMemorySongRepository(SampleSongs.all(get<DateProvider>().today())) }
        single<SetlistRepository> { InMemorySetlistRepository(SampleSetlist.around(get<DateProvider>().today())) }
        single<ExerciseRepository> { InMemoryExerciseRepository(SingerExercises.all) }
        single { InMemoryAudioStorage() }
        single<AudioStorage> { get<InMemoryAudioStorage>() }
        single<AudioCache> { DemoAudioCache() }
        single<MembersRepository> { InMemoryMembersRepository(get()) }
        single<AudioUrlProvider> { DemoAudioUrlProvider(get(), get()) }
        single<VoiceSubmissionRepository> {
            InMemoryVoiceSubmissionRepository(get(), get(), get(), SampleSubmissions.around(get<DateProvider>().today()))
        }
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
    single { signInLinkInbox }
    factoryOf(::ObserveSetlistScheduleUseCase)
    factoryOf(::DeleteSetlistUseCase)
    factoryOf(::SendVoiceUseCase)
    factoryOf(::ReviewVoiceUseCase)
    factory { VoiceRecorder(get()) }
    factoryOf(::PlayLocalAudioUseCase)
    factoryOf(::PublishSetlistUseCase)
    factoryOf(::ObserveSingerPathUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::RequestSignInCodeUseCase)
    factoryOf(::VerifySignInCodeUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::RetrySessionUseCase)
    factoryOf(::SaveSongUseCase)
    factoryOf(::ImportSongFromLinkUseCase)
    factoryOf(::ObservePlanningUseCase)
    factoryOf(::AnswerServiceUseCase)
    factoryOf(::AdjustServiceUseCase)
    factoryOf(::OpenPlanningMonthUseCase)
    factoryOf(::ValidatePlanningMonthUseCase)
    factoryOf(::DeleteSongUseCase)
    factoryOf(::RefreshContentUseCase)
    factoryOf(::ObserveSyncStatusUseCase)
    factoryOf(::ObserveBandUseCase)
    factoryOf(::InviteMemberUseCase)
    factoryOf(::ChangeMemberRoleUseCase)
    factoryOf(::RemoveMemberUseCase)
    factoryOf(::CancelInvitationUseCase)
    factoryOf(::SuggestBandWeekUseCase)
    factoryOf(::UploadRecordingUseCase)
    factoryOf(::GetTodayUseCase)
    factoryOf(::ObserveSetlistOfflineUseCase)
    factoryOf(::DownloadSetlistUseCase)
    factoryOf(::ObserveOfflineAudioUseCase)
    factoryOf(::TrackSingingUseCase)
    factoryOf(::CompleteExerciseUseCase)
    factoryOf(::PlayReferenceNoteUseCase)
}

val presentationModule = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::WeekViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ProgressViewModel)
    viewModel { params -> VoiceRecordViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModelOf(::VoiceReviewViewModel)
    viewModelOf(::SongDetailViewModel)
    viewModelOf(::SignInViewModel)
    viewModel { params -> SetlistEditorViewModel(get(), get(), get(), get(), get(), params.getOrNull()) }
    viewModelOf(::RecordingFormViewModel)
    viewModelOf(::MiniPlayerViewModel)
    viewModelOf(::ExerciseViewModel)
    viewModelOf(::MembersViewModel)
    single { StageSettingsStore() }
    viewModel { (songId: String, transpose: Int, notation: Notation) ->
        StageViewModel(songId, transpose, notation, get(), get(), get())
    }
    // The song id is optional (null = new song), which the constructor DSL cannot resolve.
    viewModelOf(::PlanningViewModel)
    viewModel {
        RemindersViewModel(get(), get(), get(), get()) {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }
    viewModel { params -> AvailabilityViewModel(params.get<LocalDate>(), get(), get(), get()) }
    viewModel { params -> ServiceDetailViewModel(params.get<LocalDate>(), get(), get(), get(), get()) }
    viewModel { params -> SongEditorViewModel(params.getOrNull<String>(), get(), get(), get(), get()) }
}

private var isKoinStarted = false

/**
 * Starts Koin once per process; safe to call from every Activity creation. [config] lets the
 * platform add what it alone can provide (the Android context).
 */
fun initKoin(config: KoinAppDeclaration = {}) {
    if (isKoinStarted) return
    startKoin {
        config()
        modules(platformModule, dataModule, domainModule, presentationModule)
    }
    isKoinStarted = true
}
