package com.xperiencemusic.shared.data.remote

import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.domain.repository.RemoteLoadStatus
import com.xperiencemusic.shared.domain.repository.SessionResettable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

/** Keeps failed initial loads retryable without terminating screen collectors. */
class RemoteLoadMonitor : RemoteLoadStatus, SessionResettable {
    private val session = SessionGuard()
    private val failed = linkedMapOf<String, suspend () -> Unit>()
    private val _hasFailure = MutableStateFlow(false)
    override val hasFailure: StateFlow<Boolean> = _hasFailure.asStateFlow()

    suspend fun load(key: String, block: suspend () -> Unit) {
        val started = session.stamp()
        try {
            block()
            if (!session.isCurrent(started)) return
            failed.remove(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!session.isCurrent(started)) return
            failed[key] = block
        }
        _hasFailure.value = failed.isNotEmpty()
    }

    override suspend fun retry() {
        failed.toMap().forEach { (key, block) -> load(key, block) }
    }

    override fun resetSession() = clear()

    fun clear() {
        session.reset()
        failed.clear()
        _hasFailure.value = false
    }
}
