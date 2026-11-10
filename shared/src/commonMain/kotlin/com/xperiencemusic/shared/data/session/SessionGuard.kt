package com.xperiencemusic.shared.data.session

import com.xperiencemusic.shared.data.remote.postgrestCall
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tells the work started under one account from the work started under the next. [reset] is
 * called when the session ends; whatever was started before it then finds its [SessionStamp]
 * outdated and must not publish or save its result.
 */
@OptIn(ExperimentalAtomicApi::class)
class SessionGuard {
    private val generation = AtomicInt(0)

    /** The session ended: every stamp taken so far is outdated. */
    fun reset() {
        generation.incrementAndFetch()
    }

    /** Marks the start of some work, to check later with [isCurrent] or [ensureCurrent]. */
    fun stamp(): SessionStamp = SessionStamp(generation.load())

    fun isCurrent(stamp: SessionStamp): Boolean = stamp.generation == generation.load()

    /** Throws a [CancellationException] when the session changed since [stamp]. */
    fun ensureCurrent(stamp: SessionStamp) {
        if (!isCurrent(stamp)) throw CancellationException(SESSION_ENDED)
    }

    /**
     * Runs [block] and returns its result, unless the session changed meanwhile: the result then
     * belongs to the previous account and a [CancellationException] is thrown instead.
     */
    suspend fun <T> call(block: suspend () -> T): T {
        val stamp = stamp()
        val result = block()
        ensureCurrent(stamp)
        return result
    }

    private companion object {
        const val SESSION_ENDED = "Session ended"
    }
}

/** The session some work started under; see [SessionGuard.stamp]. */
data class SessionStamp internal constructor(internal val generation: Int)

/** A database call of the current account: errors become domain errors, see [postgrestCall]. */
internal suspend fun <T> SessionGuard.accountCall(block: suspend () -> T): T = call { postgrestCall(block) }
