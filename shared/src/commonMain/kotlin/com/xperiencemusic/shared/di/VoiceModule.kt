package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.domain.usecase.ReviewVoiceUseCase
import com.xperiencemusic.shared.domain.usecase.SendVoiceUseCase
import com.xperiencemusic.shared.domain.usecase.VoiceRecorder
import com.xperiencemusic.shared.presentation.voice.VoiceRecordViewModel
import com.xperiencemusic.shared.presentation.voice.VoiceReviewViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** "Ma voix": singers record their part, the super admin listens and answers. */
val voiceModule = module {
    factoryOf(::VoiceRecorder)
    factoryOf(::SendVoiceUseCase)
    factoryOf(::ReviewVoiceUseCase)
    viewModel { params -> VoiceRecordViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModelOf(::VoiceReviewViewModel)
}
