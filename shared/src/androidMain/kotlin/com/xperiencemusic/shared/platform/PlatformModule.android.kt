package com.xperiencemusic.shared.platform

import androidx.media3.exoplayer.ExoPlayer
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.FileStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // First resolved from the main thread (UI start-up), which ExoPlayer requires.
    single<ExoPlayer> { AndroidAudioPlayer.buildExoPlayer(androidContext()) }
    single<AudioPlayer> { AndroidAudioPlayer(androidContext(), get()) }
    single<FileStore> { AndroidFileStore(androidContext()) }
}
