package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.remote.createXperienceSupabaseClient
import com.xperiencemusic.shared.data.repository.FileAudioCache
import com.xperiencemusic.shared.data.repository.LearningRemote
import com.xperiencemusic.shared.data.repository.OfflineFirstAudioUrlProvider
import com.xperiencemusic.shared.data.repository.SupabaseAudioStorage
import com.xperiencemusic.shared.data.repository.SupabaseAudioUrlProvider
import com.xperiencemusic.shared.data.repository.SupabaseAvatarStorage
import com.xperiencemusic.shared.data.repository.SupabaseExerciseRepository
import com.xperiencemusic.shared.data.repository.SupabaseGrowthRepository
import com.xperiencemusic.shared.data.repository.SupabaseLearningRemote
import com.xperiencemusic.shared.data.repository.SupabaseMembersRepository
import com.xperiencemusic.shared.data.repository.SupabaseObjectiveRecordingRepository
import com.xperiencemusic.shared.data.repository.SupabasePlanningRepository
import com.xperiencemusic.shared.data.repository.SupabaseSessionRepository
import com.xperiencemusic.shared.data.repository.SupabaseSetlistRepository
import com.xperiencemusic.shared.data.repository.SupabaseSongRepository
import com.xperiencemusic.shared.data.repository.SupabaseVoiceSubmissionRepository
import com.xperiencemusic.shared.data.seed.SingerExercises
import com.xperiencemusic.shared.domain.model.AppEnvironment
import com.xperiencemusic.shared.domain.repository.AudioCache
import com.xperiencemusic.shared.domain.repository.AudioStorage
import com.xperiencemusic.shared.domain.repository.AudioUrlProvider
import com.xperiencemusic.shared.domain.repository.AvatarStorage
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

/** The band's data in Supabase, with the phone's copies for offline use. */
val remoteDataModule = module {
    single { AppEnvironment(isDemo = false, demoCode = null) }
    single { createXperienceSupabaseClient() }
    single<SessionRepository> { SupabaseSessionRepository(get(), get(), get(AppScopes.Background)) }
    single { SupabaseSongRepository(get(), get()) }.resetAtSignOut() bind SongRepository::class
    single { SupabaseSetlistRepository(get(), get()) }.resetAtSignOut() bind SetlistRepository::class
    single { SupabaseExerciseRepository(get(), SingerExercises.all, get()) }.resetAtSignOut() bind ExerciseRepository::class
    single { SupabaseMembersRepository(get(), get()) }.resetAtSignOut() bind MembersRepository::class
    single { SupabaseVoiceSubmissionRepository(get(), get()) }.resetAtSignOut() bind VoiceSubmissionRepository::class
    single { SupabasePlanningRepository(get(), get()) }.resetAtSignOut() bind PlanningRepository::class
    single { SupabaseGrowthRepository(get(), get()) }.resetAtSignOut() bind GrowthRepository::class
    single { SupabaseObjectiveRecordingRepository(get(), get(), get()) }.resetAtSignOut() bind ObjectiveRecordingRepository::class
    single<AudioStorage> { SupabaseAudioStorage(get()) }
    single { SupabaseAvatarStorage(get()) }.resetAtSignOut() bind AvatarStorage::class
    single<AudioCache> { FileAudioCache(get(), get()) }
    single<AudioUrlProvider> { OfflineFirstAudioUrlProvider(get(), SupabaseAudioUrlProvider(get())) }
    single<LearningRemote> { SupabaseLearningRemote(get()) }
}
