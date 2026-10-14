package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.MicrophoneInput
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AudioPlayer> { IosAudioPlayer() }
    single<FileStore> { IosFileStore() }
    single<MicrophoneInput> { IosMicrophone() }
    single<ReminderScheduler> { IosReminderScheduler() }
}
