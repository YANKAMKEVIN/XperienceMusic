package com.xperiencemusic.shared.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

/** Keeps failed initial loads retryable without terminating screen collectors. */
class RemoteLoadMonitor {
    private var generation = 0
    private val failed = linkedMapOf<String, suspend () -> Unit>()
    private val _hasFailure = MutableStateFlow(false)
    val hasFailure = _hasFailure.asStateFlow()

    suspend fun load(key: String, block: suspend () -> Unit) {
        val started = generation
        try {
            block()
            if (started != generation) return
            failed.remove(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (started != generation) return
            failed[key] = block
        }
        _hasFailure.value = failed.isNotEmpty()
    }

    suspend fun retry() {
        failed.toMap().forEach { (key, block) -> load(key, block) }
    }

    fun clear() {
        generation++
        failed.clear()
        _hasFailure.value = false
    }
}
