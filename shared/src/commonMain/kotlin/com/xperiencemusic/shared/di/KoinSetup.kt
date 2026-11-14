package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.remote.SupabaseConfig
import com.xperiencemusic.shared.platform.platformModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/** The modules of every feature, whichever data source backs them. */
val featureModules: List<Module> = listOf(
    sessionModule,
    songsModule,
    setlistsModule,
    planningModule,
    growthModule,
    learningModule,
    voiceModule,
    membersModule,
    playerModule,
)

/**
 * Everything the app needs: Supabase when `local.properties` holds the project keys
 * ([isDemo] false), otherwise the offline demo.
 */
fun appModules(isDemo: Boolean = !SupabaseConfig.isConfigured): List<Module> =
    listOf(platformModule, coreModule, if (isDemo) demoDataModule else remoteDataModule) + featureModules

private var isKoinStarted = false

/**
 * Starts Koin once per process: Android calls it from its `Application`, iOS from each
 * `MainViewController`; later calls do nothing. [config] lets the platform add what it alone can
 * provide (the Android context).
 */
fun initKoin(config: KoinAppDeclaration = {}) {
    if (isKoinStarted) return
    startKoin {
        config()
        modules(appModules())
    }
    isKoinStarted = true
}
