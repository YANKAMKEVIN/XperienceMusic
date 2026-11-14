package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.local.FileLearningSessionStore
import com.xperiencemusic.shared.data.repository.FileLearningRepository
import com.xperiencemusic.shared.domain.learning.LearningCatalog
import com.xperiencemusic.shared.domain.repository.LearningRepository
import com.xperiencemusic.shared.domain.repository.LearningSessionStore
import com.xperiencemusic.shared.domain.usecase.AnalyzePianoScaleUseCase
import com.xperiencemusic.shared.domain.usecase.GetLearningPreferencesUseCase
import com.xperiencemusic.shared.domain.usecase.GradeActivityUseCase
import com.xperiencemusic.shared.domain.usecase.LoadLearningSessionUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveLearningProgressUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveChecksUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveObjectiveRecordingsUseCase
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
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** The guided learning path, its activities, the scale workshop and the recorded objectives. */
val learningModule = module {
    single { FileLearningRepository(get(), get()) }.resetAtSignOut() bind LearningRepository::class
    single<LearningSessionStore> { FileLearningSessionStore(get()) }
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
    factoryOf(::PlayLearningExampleUseCase)
    factory { AnalyzePianoScaleUseCase() }
    factoryOf(::SingingSessionController)
    factoryOf(::LearningDependencies)
    viewModel { params ->
        LearningViewModel(
            moduleId = params.getOrNull<String>() ?: LearningCatalog.DEFAULT,
            observeMember = get(),
            dependencies = get(),
        )
    }
    viewModel { params -> AssessmentViewModel(params.get(), get(), get(), get()) }
    viewModelOf(::PianoScaleViewModel)
}
