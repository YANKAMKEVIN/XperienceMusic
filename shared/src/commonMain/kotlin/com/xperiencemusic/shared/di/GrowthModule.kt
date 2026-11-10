package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.seed.GrowthObjectives
import com.xperiencemusic.shared.domain.usecase.ChangeMemberLevelUseCase
import com.xperiencemusic.shared.domain.usecase.CompleteExerciseUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveGrowthReviewUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveMyGrowthUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSingerPathUseCase
import com.xperiencemusic.shared.domain.usecase.PlayReferenceNoteUseCase
import com.xperiencemusic.shared.domain.usecase.RequestLevelUseCase
import com.xperiencemusic.shared.domain.usecase.ReviewLevelRequestUseCase
import com.xperiencemusic.shared.domain.usecase.ReviewObjectiveUseCase
import com.xperiencemusic.shared.domain.usecase.TickObjectiveUseCase
import com.xperiencemusic.shared.domain.usecase.TrackSingingUseCase
import com.xperiencemusic.shared.presentation.exercise.ExerciseViewModel
import com.xperiencemusic.shared.presentation.progress.GrowthReviewViewModel
import com.xperiencemusic.shared.presentation.progress.ProgressViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The Progress tab: levels and their objectives, the singers' path and its exercises. */
val growthModule = module {
    factory { ObserveMyGrowthUseCase(get(), get(), get(), GrowthObjectives.all) }
    factory { TickObjectiveUseCase(get(), get(), GrowthObjectives.all) }
    factoryOf(::RequestLevelUseCase)
    factoryOf(::ObserveGrowthReviewUseCase)
    factoryOf(::ReviewLevelRequestUseCase)
    factoryOf(::ReviewObjectiveUseCase)
    factoryOf(::ChangeMemberLevelUseCase)
    factoryOf(::ObserveSingerPathUseCase)
    factoryOf(::TrackSingingUseCase)
    factoryOf(::CompleteExerciseUseCase)
    factoryOf(::PlayReferenceNoteUseCase)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::ExerciseViewModel)
    viewModel { GrowthReviewViewModel(get(), GrowthObjectives.all, get(), get(), get(), get()) }
}
