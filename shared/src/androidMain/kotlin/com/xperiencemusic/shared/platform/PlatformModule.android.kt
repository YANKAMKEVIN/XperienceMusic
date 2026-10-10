package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.TempFileWriter
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AudioPlayer> { AndroidAudioPlayer(androidContext()) }
    single<TempFileWriter> { AndroidTempFileWriter(androidContext()) }
}
