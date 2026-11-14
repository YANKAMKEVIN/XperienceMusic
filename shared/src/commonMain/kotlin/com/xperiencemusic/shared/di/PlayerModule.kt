package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.local.FileMicrophoneIntroStore
import com.xperiencemusic.shared.domain.repository.MicrophoneIntroStore
import com.xperiencemusic.shared.domain.usecase.MarkMicrophoneExplainedUseCase
import com.xperiencemusic.shared.domain.usecase.PcmRecorder
import com.xperiencemusic.shared.domain.usecase.PlayLocalAudioUseCase
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import com.xperiencemusic.shared.domain.usecase.WasMicrophoneExplainedUseCase
import com.xperiencemusic.shared.presentation.player.MiniPlayerViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The app's audio: the shared player, local previews and the microphone. */
val playerModule = module {
    // Drives the platform players, which expect the main thread.
    single { PlaybackController(get(), get(), get(AppScopes.Main)) }.resetAtSignOut()
    factoryOf(::PlayLocalAudioUseCase)
    factoryOf(::PcmRecorder)
    single<MicrophoneIntroStore> { FileMicrophoneIntroStore(get()) }
    factoryOf(::WasMicrophoneExplainedUseCase)
    factoryOf(::MarkMicrophoneExplainedUseCase)
    viewModelOf(::MiniPlayerViewModel)
}
