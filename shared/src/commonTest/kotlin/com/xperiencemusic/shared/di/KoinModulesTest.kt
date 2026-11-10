package com.xperiencemusic.shared.di

import com.xperiencemusic.music.theory.Notation
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.repository.LearningRepository
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import com.xperiencemusic.shared.domain.repository.ObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.repository.PlanningRepository
import com.xperiencemusic.shared.domain.repository.SessionResettable
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.fakes.FakeAudioPlayer
import com.xperiencemusic.shared.fakes.FakeFileStore
import com.xperiencemusic.shared.fakes.FakeMicrophone
import com.xperiencemusic.shared.platform.LocalNotification
import com.xperiencemusic.shared.platform.NotificationScheduler
import com.xperiencemusic.shared.presentation.account.AccountViewModel
import com.xperiencemusic.shared.presentation.app.AppViewModel
import com.xperiencemusic.shared.presentation.exercise.ExerciseViewModel
import com.xperiencemusic.shared.presentation.learning.AssessmentViewModel
import com.xperiencemusic.shared.presentation.learning.LearningViewModel
import com.xperiencemusic.shared.presentation.learning.PianoScaleViewModel
import com.xperiencemusic.shared.presentation.library.LibraryViewModel
import com.xperiencemusic.shared.presentation.members.MembersViewModel
import com.xperiencemusic.shared.presentation.onboarding.OnboardingViewModel
import com.xperiencemusic.shared.presentation.planning.AvailabilityViewModel
import com.xperiencemusic.shared.presentation.planning.PlanningViewModel
import com.xperiencemusic.shared.presentation.planning.ServiceDetailViewModel
import com.xperiencemusic.shared.presentation.player.MiniPlayerViewModel
import com.xperiencemusic.shared.presentation.profile.ProfileViewModel
import com.xperiencemusic.shared.presentation.progress.GrowthReviewViewModel
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import com.xperiencemusic.shared.presentation.recordingform.RecordingFormViewModel
import com.xperiencemusic.shared.presentation.setlisteditor.SetlistEditorViewModel
import com.xperiencemusic.shared.presentation.signin.SignInViewModel
import com.xperiencemusic.shared.presentation.song.SongDetailViewModel
import com.xperiencemusic.shared.presentation.songeditor.SongEditorViewModel
import com.xperiencemusic.shared.presentation.stage.StageViewModel
import com.xperiencemusic.shared.presentation.voice.VoiceRecordViewModel
import com.xperiencemusic.shared.presentation.voice.VoiceReviewViewModel
import com.xperiencemusic.shared.presentation.week.WeekViewModel
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The demo wiring, with fakes for what only the phone provides. */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinModulesTest {
    private val fakePlatformModule = module {
        single<AudioPlayer> { FakeAudioPlayer() }
        single<FileStore> { FakeFileStore() }
        single<MicrophoneInput> { FakeMicrophone(emptyList()) }
        single<NotificationScheduler> { SilentNotificationScheduler() }
    }

    private lateinit var app: KoinApplication
    private val koin: Koin get() = app.koin

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        app = koinApplication { modules(listOf(fakePlatformModule, coreModule, demoDataModule) + featureModules) }
    }

    @AfterTest
    fun tearDown() {
        app.close()
        Dispatchers.resetMain()
    }

    @Test
    fun everyViewModelResolvesInTheDemo() {
        val date = LocalDate(2026, 11, 1)
        val viewModels = listOf(
            koin.get<AppViewModel>(),
            koin.get<SignInViewModel>(),
            koin.get<OnboardingViewModel>(),
            koin.get<ProfileViewModel>(),
            koin.get<AccountViewModel>(),
            koin.get<WeekViewModel>(),
            koin.get<SetlistEditorViewModel> { parametersOf(null) },
            koin.get<SetlistEditorViewModel> { parametersOf(date) },
            koin.get<LibraryViewModel>(),
            koin.get<SongDetailViewModel> { parametersOf("song") },
            koin.get<SongEditorViewModel> { parametersOf(null) },
            koin.get<SongEditorViewModel> { parametersOf("song") },
            koin.get<RecordingFormViewModel> { parametersOf("song") },
            koin.get<StageViewModel> { parametersOf("song", 0, Notation.NASHVILLE) },
            koin.get<PlanningViewModel>(),
            koin.get<AvailabilityViewModel> { parametersOf(date) },
            koin.get<ServiceDetailViewModel> { parametersOf(date) },
            koin.get<ProgressViewModel>(),
            koin.get<ExerciseViewModel> { parametersOf("exercise") },
            koin.get<GrowthReviewViewModel>(),
            koin.get<LearningViewModel> { parametersOf(null) },
            koin.get<AssessmentViewModel> { parametersOf("piano-chords") },
            koin.get<PianoScaleViewModel>(),
            koin.get<VoiceRecordViewModel> { parametersOf("song") },
            koin.get<VoiceReviewViewModel>(),
            koin.get<MembersViewModel>(),
            koin.get<MiniPlayerViewModel>(),
        )
        assertEquals(27, viewModels.size)
    }

    @Test
    fun everySessionScopedRepositoryIsResetAtSignOut() {
        val resettable = koin.getAll<SessionResettable>()
        val repositories = listOf(
            koin.get<SongRepository>(),
            koin.get<SetlistRepository>(),
            koin.get<ExerciseRepository>(),
            koin.get<MembersRepository>(),
            koin.get<PlanningRepository>(),
            koin.get<GrowthRepository>(),
            koin.get<VoiceSubmissionRepository>(),
            koin.get<LearningRepository>(),
            koin.get<ObjectiveRecordingRepository>(),
            koin.get<PlaybackController>(),
            koin.get<RemoteLoadMonitor>(),
        )
        repositories.filterIsInstance<SessionResettable>().forEach { repository ->
            assertTrue(resettable.any { it === repository }, "${repository::class.simpleName} is not reset at sign-out")
        }
        assertTrue(resettable.any { it is LearningRepository })
        assertTrue(resettable.any { it is ObjectiveRecordingRepository })
    }

    @Test
    fun everySupabaseRepositoryIsResetAtSignOut() {
        // Nothing is fetched: building the repositories needs a client, not a server.
        val offlineClient = module {
            single { createSupabaseClient("https://example.invalid", "key") { install(Postgrest); install(Storage) } }
        }
        val remote = koinApplication {
            modules(listOf(fakePlatformModule, coreModule, remoteDataModule, offlineClient) + featureModules)
        }.koin
        val resettable = remote.getAll<SessionResettable>()
        val repositories = listOf(
            remote.get<SongRepository>(),
            remote.get<SetlistRepository>(),
            remote.get<ExerciseRepository>(),
            remote.get<MembersRepository>(),
            remote.get<PlanningRepository>(),
            remote.get<GrowthRepository>(),
            remote.get<VoiceSubmissionRepository>(),
            remote.get<LearningRepository>(),
            remote.get<ObjectiveRecordingRepository>(),
            remote.get<AvatarStorage>(),
        )
        repositories.forEach { repository ->
            assertTrue(repository is SessionResettable, "${repository::class.simpleName} keeps account data")
            assertTrue(resettable.any { it === repository }, "${repository::class.simpleName} is not reset at sign-out")
        }
    }

    @Test
    fun signingOutResetsEverySessionScopedDefinition() = runTest {
        val reset = mutableListOf<String>()
        val spies = module {
            single(named("first")) { Spy("first", reset) }.resetAtSignOut()
            single(named("second")) { Spy("second", reset) }.resetAtSignOut()
        }
        app.modules(spies)

        koin.get<LocalDataStore>().clearAll()

        assertEquals(listOf("first", "second"), reset.sorted())
    }

    private class Spy(private val name: String, private val reset: MutableList<String>) : SessionResettable {
        override fun resetSession() {
            reset += name
        }
    }

    private class SilentNotificationScheduler : NotificationScheduler {
        override suspend fun replaceAll(notifications: List<LocalNotification>) = Unit
    }
}
