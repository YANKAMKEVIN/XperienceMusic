package com.xperiencemusic.shared.di

import com.xperiencemusic.shared.data.local.DeviceDataStore
import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.local.SystemDateProvider
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.domain.repository.DateProvider
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.repository.RemoteLoadStatus
import com.xperiencemusic.shared.domain.repository.SessionResettable
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

/** What every feature shares, whichever data source the app runs on. */
val coreModule = module {
    single(AppScopes.Background) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single(AppScopes.Main) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single { HttpClient() }
    single<DateProvider> { SystemDateProvider() }
    single { SnapshotStore(get()) }
    single { RemoteLoadMonitor() }.resetAtSignOut() bind RemoteLoadStatus::class
    // Every definition bound to SessionResettable drops its account data at sign-out.
    single<LocalDataStore> { DeviceDataStore(get(), get(), get(), get()) { getAll<SessionResettable>() } }
}
