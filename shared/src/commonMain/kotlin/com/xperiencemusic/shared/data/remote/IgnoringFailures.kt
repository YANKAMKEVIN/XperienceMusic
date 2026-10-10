package com.xperiencemusic.shared.data.remote

import kotlin.coroutines.cancellation.CancellationException

/**
 * For background loads started by a Flow collector: a network failure must not crash the
 * collecting ViewModel. The screen keeps showing its loading state until the next attempt.
 */
internal suspend fun ignoringFailures(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Unit
    }
}
