package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.repository.DemoAudioCache
import com.xperiencemusic.shared.data.repository.DemoAudioUrlProvider
import com.xperiencemusic.shared.data.repository.InMemoryAudioStorage
import com.xperiencemusic.shared.data.repository.InMemoryAvatarStorage
import com.xperiencemusic.shared.data.repository.InMemoryExerciseRepository
import com.xperiencemusic.shared.data.repository.InMemoryGrowthRepository
import com.xperiencemusic.shared.data.repository.InMemoryLearningRemote
import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.repository.InMemoryObjectiveRecordingRepository
import com.xperiencemusic.shared.data.repository.InMemoryPlanningRepository
import com.xperiencemusic.shared.data.repository.InMemorySessionRepository
import com.xperiencemusic.shared.data.repository.InMemorySetlistRepository
import com.xperiencemusic.shared.data.repository.InMemorySongRepository
import com.xperiencemusic.shared.data.repository.InMemoryVoiceSubmissionRepository
import com.xperiencemusic.shared.data.repository.LearningRemote
import com.xperiencemusic.shared.data.seed.SampleGrowth
import com.xperiencemusic.shared.data.seed.SamplePlanning
import com.xperiencemusic.shared.data.seed.SampleSetlist
import com.xperiencemusic.shared.data.seed.SampleSongs
import com.xperiencemusic.shared.data.seed.SampleSubmissions
import com.xperiencemusic.shared.data.seed.SingerExercises
import com.xperiencemusic.shared.domain.model.AppEnvironment
import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.domain.repository.AvatarStorage
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.ExerciseRepository
import com.xperiencemusic.shared.domain.repository.GrowthRepository
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.ObjectiveRecordingRepository
import com.xperiencemusic.shared.domain.repository.PlanningRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.repository.SetlistRepository
import com.xperiencemusic.shared.domain.repository.SongRepository
import com.xperiencemusic.shared.domain.repository.VoiceSubmissionRepository
import org.koin.dsl.bind
import org.koin.dsl.module

/** The offline demo: sample data around today, kept in memory (the session on the phone). */
val demoDataModule = module {
    single { AppEnvironment(isDemo = true, demoCode = InMemorySessionRepository.DEMO_CODE) }
    single<SessionRepository> { InMemorySessionRepository(files = get()) }
    single<SongRepository> { InMemorySongRepository(SampleSongs.all(get<DateProvider>().today())) }
    single<SetlistRepository> { InMemorySetlistRepository(SampleSetlist.around(get<DateProvider>().today())) }
    single<ExerciseRepository> { InMemoryExerciseRepository(SingerExercises.all) }
    single<MembersRepository> { InMemoryMembersRepository(get()) }
    single<VoiceSubmissionRepository> {
        InMemoryVoiceSubmissionRepository(get(), get(), get(), SampleSubmissions.around(get<DateProvider>().today()))
    }
    single<PlanningRepository> {
        val today = get<DateProvider>().today()
        InMemoryPlanningRepository(SamplePlanning.months(today), SamplePlanning.signups(today))
    }
    single<GrowthRepository> {
        val today = get<DateProvider>().today()
        InMemoryGrowthRepository(get(), get(), get(), SampleGrowth.requests(today), SampleGrowth.checks(today))
    }
    single { InMemoryObjectiveRecordingRepository(get()) }.resetAtSignOut() bind ObjectiveRecordingRepository::class
    single { InMemoryAudioStorage() } bind AudioStorage::class
    single<AvatarStorage> { InMemoryAvatarStorage() }
    single<AudioCache> { DemoAudioCache() }
    single<AudioUrlProvider> { DemoAudioUrlProvider(get(), get()) }
    single<LearningRemote> { InMemoryLearningRemote() }
}
