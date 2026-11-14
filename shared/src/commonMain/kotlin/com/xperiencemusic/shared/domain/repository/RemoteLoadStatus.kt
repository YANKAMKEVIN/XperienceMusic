package com.xperiencemusic.shared.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * The background loads of the data the phone keeps no copy of (planning, members, growth, voice
 * submissions, objective recordings): whether one of them failed, and a way to run the failed
 * ones again. Without them a screen can be incomplete, so the app says so above every tab.
 *
 * Songs and setlists report through their own `SyncStatus` instead, on purpose: they are
 * offline-first, so a failed refresh still leaves the last copy on screen, which the Week and
 * Library tabs show as "offline" and refresh by pulling down. Folding them in here would raise
 * the banner over a complete, merely older copy, and the two messages would say the same thing
 * twice; `SongRepository.syncStatus` is also part of an interface another branch is changing.
 */
interface RemoteLoadStatus {
    /** True while at least one load failed and was not retried successfully. */
    val hasFailure: StateFlow<Boolean>

    /** Runs every failed load again. */
    suspend fun retry()
}
