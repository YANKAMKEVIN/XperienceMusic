package com.xperiencemusic.shared.data.learning

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.domain.learning.*
import com.xperiencemusic.shared.domain.repository.SessionResettable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.cancellation.CancellationException

interface LearningRemote {
    suspend fun fetch(memberId: String): List<LearningAttempt>
    suspend fun send(attempt: LearningAttempt)
}

@Serializable
private data class StoredAttempt(val attempt: LearningAttempt, val synced: Boolean = false)

/** Attempts are written locally first. Network failures leave an explicit, retryable outbox. */
class FileLearningRepository(
    private val snapshots: SnapshotStore,
    private val remote: LearningRemote? = null,
) : LearningRepository, SessionResettable {
    private val stored = MutableStateFlow<Map<String, List<StoredAttempt>>>(emptyMap())
    private val lock = Mutex()
    private var generation = 0

    override fun resetSession() {
        generation++
        stored.value = emptyMap()
    }

    private fun load(memberId: String) {
        if (memberId !in stored.value) {
            val rows = snapshots.load("learning-$memberId", ListSerializer(StoredAttempt.serializer())).orEmpty()
            stored.value = stored.value + (memberId to rows)
        }
    }

    private fun save(memberId: String, rows: List<StoredAttempt>) {
        snapshots.save("learning-$memberId", ListSerializer(StoredAttempt.serializer()), rows)
        stored.value = stored.value + (memberId to rows)
    }

    override fun observe(memberId: String): Flow<LearningProgress> = stored
        .onStart { load(memberId) }
        .map { data ->
            val rows = data[memberId].orEmpty()
            LearningProgress(rows.map { it.attempt }, rows.count { !it.synced && remote != null })
        }

    override suspend fun record(attempt: LearningAttempt) {
        val started = generation
        lock.withLock {
            if (started != generation) throw CancellationException("Session ended")
            load(attempt.memberId)
            val rows = stored.value[attempt.memberId].orEmpty()
            save(attempt.memberId, rows + StoredAttempt(attempt, synced = remote == null))
        }
    }

    override suspend fun sync(memberId: String) {
        val source = remote ?: return
        val started = generation
        try {
            // Network calls never hold the local write lock: training remains usable offline.
            val fetched = source.fetch(memberId)
            lock.withLock {
                if (started != generation) return
                load(memberId)
                val local = stored.value[memberId].orEmpty()
                val merged = fetched.map { StoredAttempt(it, true) }.associateBy { it.attempt.id }.toMutableMap()
                local.forEach { if (it.attempt.id !in merged) merged[it.attempt.id] = it }
                save(memberId, merged.values.toList())
            }
            val pending = stored.value[memberId].orEmpty().filter { !it.synced }
            pending.forEach { item ->
                if (started != generation) return
                source.send(item.attempt)
                lock.withLock {
                    if (started != generation) return
                    save(memberId, stored.value[memberId].orEmpty().map {
                        if (it.attempt.id == item.attempt.id) it.copy(synced = true) else it
                    })
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The local attempt is durable; its pending marker offers a manual retry.
        }
    }
}
