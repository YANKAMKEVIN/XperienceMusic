package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.local.StoredAttemptDto
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.mapper.toStored
import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.domain.model.LearningProgress
import com.xperiencemusic.shared.domain.repository.LearningRepository
import com.xperiencemusic.shared.domain.repository.SessionResettable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.cancellation.CancellationException

/** Attempts are written locally first. Network failures leave an explicit, retryable outbox. */
class FileLearningRepository(
    private val snapshots: SnapshotStore,
    private val remote: LearningRemote,
) : LearningRepository, SessionResettable {
    private val stored = MutableStateFlow<Map<String, List<StoredAttemptDto>>>(emptyMap())
    private val lock = Mutex()
    private val session = SessionGuard()

    override fun resetSession() {
        session.reset()
        stored.value = emptyMap()
    }

    private fun load(memberId: String) {
        if (memberId !in stored.value) {
            val rows = snapshots.load(key(memberId), ListSerializer(StoredAttemptDto.serializer())).orEmpty()
            stored.value = stored.value + (memberId to rows)
        }
    }

    private fun save(memberId: String, rows: List<StoredAttemptDto>) {
        snapshots.save(key(memberId), ListSerializer(StoredAttemptDto.serializer()), rows)
        stored.value = stored.value + (memberId to rows)
    }

    override fun observe(memberId: String): Flow<LearningProgress> = stored
        .onStart { load(memberId) }
        .map { data ->
            val rows = data[memberId].orEmpty()
            LearningProgress(rows.map { it.attempt.toDomain() }, rows.count { !it.synced })
        }

    override suspend fun record(attempt: LearningAttempt) {
        val started = session.stamp()
        lock.withLock {
            session.ensureCurrent(started)
            load(attempt.memberId)
            val rows = stored.value[attempt.memberId].orEmpty()
            save(attempt.memberId, rows + StoredAttemptDto(attempt.toStored()))
        }
    }

    override suspend fun sync(memberId: String) {
        val started = session.stamp()
        try {
            // Network calls never hold the local write lock: training remains usable offline.
            val fetched = remote.fetch(memberId)
            lock.withLock {
                if (!session.isCurrent(started)) return
                load(memberId)
                val local = stored.value[memberId].orEmpty()
                val merged = fetched.map { StoredAttemptDto(it.toStored(), synced = true) }.associateBy { it.attempt.id }.toMutableMap()
                local.forEach { if (it.attempt.id !in merged) merged[it.attempt.id] = it }
                save(memberId, merged.values.toList())
            }
            val pending = stored.value[memberId].orEmpty().filter { !it.synced }
            pending.forEach { item ->
                if (!session.isCurrent(started)) return
                remote.send(item.attempt.toDomain())
                lock.withLock {
                    if (!session.isCurrent(started)) return
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

    private fun key(memberId: String) = "learning-$memberId"
}
