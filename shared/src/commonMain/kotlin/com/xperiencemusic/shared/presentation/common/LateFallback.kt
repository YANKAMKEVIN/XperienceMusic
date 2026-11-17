package com.xperiencemusic.shared.presentation.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * This flow, with [fallback] emitted first only when nothing came within [grace].
 *
 * A screen that must stay usable offline can show what it knows locally, but showing it at once
 * makes it flash every time the full data is a moment behind: half a screen appears, then is
 * replaced. Waiting [grace] keeps the loader up for that moment instead.
 */
fun <T> Flow<T>.withLateFallback(grace: Duration, fallback: () -> T): Flow<T> = channelFlow {
    val late = launch {
        delay(grace)
        send(fallback())
    }
    collect { value ->
        late.cancel()
        send(value)
    }
}
