package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.data.local.FileLearningSessionStore
import com.xperiencemusic.shared.data.repository.FileLearningRepository
import com.xperiencemusic.shared.data.repository.InMemoryLearningRemote
import com.xperiencemusic.shared.data.repository.InMemoryObjectiveRecordingRepository
import com.xperiencemusic.shared.data.repository.LearningRemote
import com.xperiencemusic.shared.data.repository.SupabaseLearningRemote
import com.xperiencemusic.shared.data.repository.SupabaseObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.repository.LearningRepository
import com.xperiencemusic.shared.domain.repository.LearningSessionStore
import com.xperiencemusic.shared.domain.repository.ObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.usecase.AnalyzePianoScaleUseCase
import com.xperiencemusic.shared.domain.usecase.GetLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.GradeActivityUseCase
import com.xperiencemusic.shared.domain.usecase.LoadLearningSessionUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveLearningProgressUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveChecksUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveRecordingsUseCase
import com.xperiencemusic.shared.domain.usecase.PcmRecorder
import com.xperiencemusic.shared.domain.usecase.PlayLearningExampleUseCase
import com.xperiencemusic.shared.domain.usecase.RecordLearningAttemptUseCase
import com.xperiencemusic.shared.domain.usecase.SaveLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.SaveLearningResumeUseCase
import com.xperiencemusic.shared.domain.usecase.SavePostponedActivitiesUseCase
import com.xperiencemusic.shared.domain.usecase.SubmitObjectiveRecordingUseCase
import com.xperiencemusic.shared.domain.usecase.SyncLearningUseCase
import com.xperiencemusic.shared.presentation.learning.AssessmentViewModel
import com.xperiencemusic.shared.presentation.learning.LearningDependencies
import com.xperiencemusic.shared.presentation.learning.LearningViewModel
import com.xperiencemusic.shared.presentation.learning.PianoScaleViewModel
import com.xperiencemusic.shared.presentation.learning.SingingSessionController
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
import com.xperiencemusic.shared.data.repository.SupabaseAvatarStorage
import com.xperiencemusic.shared.data.repository.InMemoryAvatarStorage
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import com.xperiencemusic.shared.domain.usecase.ChangeAvatarUseCase
import com.xperiencemusic.shared.presentation.account.AccountViewModel
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
import com.xperiencemusic.shared.domain.usecase.UpdateMyProfileUseCase
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
import com.xperiencemusic.shared.presentation.profile.ProfileViewModel
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
import com.xperiencemusic.shared.presentation.progress.GrowthReviewViewModel
import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.SupabaseGrowthRepository
import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.data.seed.SampleGrowth
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.usecase.ChangeMemberLevelUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveGrowthReviewUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveMyGrowthUseCase
import com.xperiencemusic.shared.domain.usecase.RequestLevelUseCase
import com.xperiencemusic.shared.domain.usecase.ReviewLevelRequestUseCase
import com.xperiencemusic.shared.domain.usecase.ReviewObjectiveUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
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
    single<LearningRepository> { FileLearningRepository(get(), get()) }
    single<LearningSessionStore> { FileLearningSessionStore(get()) }
    single { com.xperiencemusic.shared.data.remote.RemoteLoadMonitor() }
    single<LocalDataStore> {
        DeviceDataStore(get(), get(), get(), get()) {
            get<PlaybackController>().stop()
            listOf(get<SongRepository>(), get<SetlistRepository>(), get<ExerciseRepository>(),
                get<MembersRepository>(), get<PlanningRepository>(), get<GrowthRepository>(),
                get<VoiceSubmissionRepository>(), get<LearningRepository>(), get<ObjectiveRecordingRepository>()).forEach {
                (it as? com.xperiencemusic.shared.domain.repository.SessionResettable)?.resetSession()
            }
            get<com.xperiencemusic.shared.data.remote.RemoteLoadMonitor>().clear()
        }
    }
    single<AnnouncedMonthsStore> { FileAnnouncedMonthsStore(get()) }
    single<com.xperiencemusic.shared.domain.repository.MicrophoneIntroStore> { com.xperiencemusic.shared.data.local.FileMicrophoneIntroStore(get()) }
    if (SupabaseConfig.isConfigured) {
        single { AppEnvironment(isDemo = false, demoCode = null) }
        single { createXperienceSupabaseClient() }
        single<SessionRepository> { SupabaseSessionRepository(get(), get(), get()) }
        single<SongRepository> { SupabaseSongRepository(get(), get()) }
        single<SetlistRepository> { SupabaseSetlistRepository(get(), get()) }
        single<ExerciseRepository> { SupabaseExerciseRepository(get(), SingerExercises.all, get()) }
        single<AudioStorage> { SupabaseAudioStorage(get()) }
        single<AvatarStorage> { SupabaseAvatarStorage(get()) }
        single<AudioCache> { FileAudioCache(get(), get()) }
        single<MembersRepository> { SupabaseMembersRepository(get(), get()) }
        single<AudioUrlProvider> { OfflineFirstAudioUrlProvider(get(), SupabaseAudioUrlProvider(get())) }
        single<VoiceSubmissionRepository> { SupabaseVoiceSubmissionRepository(get(), get()) }
        single<PlanningRepository> { SupabasePlanningRepository(get(), get()) }
        single<GrowthRepository> { SupabaseGrowthRepository(get(), get()) }
        single<LearningRemote> { SupabaseLearningRemote(get()) }
        single<ObjectiveRecordingRepository> { SupabaseObjectiveRecordingRepository(get(), get(), get()) }
    } else {
        single { AppEnvironment(isDemo = true, demoCode = InMemorySessionRepository.DEMO_CODE) }
        single<SessionRepository> { InMemorySessionRepository(files = get()) }
        single<GrowthRepository> {
            val today = get<DateProvider>().today()
            InMemoryGrowthRepository(get(), get(), get(), SampleGrowth.requests(today), SampleGrowth.checks(today))
        }
        single<PlanningRepository> {
            val today = get<DateProvider>().today()
            InMemoryPlanningRepository(SamplePlanning.months(today), SamplePlanning.signups(today))
        }
        single<SongRepository> { InMemorySongRepository(SampleSongs.all(get<DateProvider>().today())) }
        single<SetlistRepository> { InMemorySetlistRepository(SampleSetlist.around(get<DateProvider>().today())) }
        single<ExerciseRepository> { InMemoryExerciseRepository(SingerExercises.all) }
        single { InMemoryAudioStorage() }
        single<AudioStorage> { get<InMemoryAudioStorage>() }
        single<AvatarStorage> { InMemoryAvatarStorage() }
        single<AudioCache> { DemoAudioCache() }
        single<MembersRepository> { InMemoryMembersRepository(get()) }
        single<AudioUrlProvider> { DemoAudioUrlProvider(get(), get()) }
        single<VoiceSubmissionRepository> {
            InMemoryVoiceSubmissionRepository(get(), get(), get(), SampleSubmissions.around(get<DateProvider>().today()))
        }
        single<LearningRemote> { InMemoryLearningRemote() }
        single<ObjectiveRecordingRepository> { InMemoryObjectiveRecordingRepository(get()) }
    }
}

val domainModule = module {
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::UpdateMyProfileUseCase)
    factoryOf(::ChangeAvatarUseCase)
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
    factory { ObserveMyGrowthUseCase(get(), get(), get(), GrowthObjectives.all) }
    factoryOf(::RequestLevelUseCase)
    factory { TickObjectiveUseCase(get(), get(), GrowthObjectives.all) }
    factoryOf(::ObserveGrowthReviewUseCase)
    factoryOf(::ReviewLevelRequestUseCase)
    factoryOf(::ReviewObjectiveUseCase)
    factoryOf(::ChangeMemberLevelUseCase)
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
    factoryOf(::PlayLearningExampleUseCase)
    factoryOf(::PcmRecorder)
    factory { AnalyzePianoScaleUseCase() }
    factoryOf(::ObserveLearningProgressUseCase)
    factoryOf(::SyncLearningUseCase)
    factoryOf(::RecordLearningAttemptUseCase)
    factoryOf(::GradeActivityUseCase)
    factoryOf(::ObserveObjectiveChecksUseCase)
    factoryOf(::ObserveObjectiveRecordingsUseCase)
    factoryOf(::SubmitObjectiveRecordingUseCase)
    factoryOf(::GetLearningPreferencesUseCase)
    factoryOf(::SaveLearningPreferencesUseCase)
    factoryOf(::LoadLearningSessionUseCase)
    factoryOf(::SaveLearningResumeUseCase)
    factoryOf(::SavePostponedActivitiesUseCase)
}

val presentationModule = module {
    factoryOf(::SingingSessionController)
    factoryOf(::LearningDependencies)
    viewModelOf(::PianoScaleViewModel)
    viewModel { params ->
        LearningViewModel(
            moduleId = params.getOrNull<String>() ?: LearningCatalog.DEFAULT,
            observeMember = get(),
            dependencies = get(),
        )
    }
    viewModel { params -> AssessmentViewModel(params.get(), get(), get(), get()) }
    viewModelOf(::AppViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::WeekViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::ProgressViewModel)
    viewModel { GrowthReviewViewModel(get(), GrowthObjectives.all, get(), get(), get(), get()) }
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
 * Starts Koin once per process: Android calls it from its `Application`, iOS from each
 * `MainViewController`; later calls do nothing. [config] lets the platform add what it alone can
 * provide (the Android context).
 */
fun initKoin(config: KoinAppDeclaration = {}) {
    if (isKoinStarted) return
    startKoin {
        config()
        modules(platformModule, dataModule, domainModule, presentationModule)
    }
    isKoinStarted = true
}
