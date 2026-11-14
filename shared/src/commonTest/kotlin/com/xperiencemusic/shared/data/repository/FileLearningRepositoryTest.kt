package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.domain.learning.PianoBasics
import com.xperiencemusic.shared.domain.model.LearningAttempt
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class FileLearningRepositoryTest {
    private val attempt = LearningAttempt("a1", "me", PianoBasics.activities.first().id, 1, true, LocalDate(2026, 10, 18))
    private class Remote : LearningRemote {
        var offline = true
        val rows = mutableMapOf<String, LearningAttempt>()
        override suspend fun fetch(memberId: String): List<LearningAttempt> {
            check(!offline)
            return rows.values.filter { it.memberId == memberId }
        }
        override suspend fun send(attempt: LearningAttempt) { check(!offline); rows[attempt.id] = attempt }
    }

    @Test fun offlineAttemptSurvivesRestartThenSyncsOnlyOnce() = runTest {
        val files = FakeFileStore()
        val remote = Remote()
        val first = FileLearningRepository(SnapshotStore(files), remote)
        first.record(attempt)
        first.sync("me")
        val restarted = FileLearningRepository(SnapshotStore(files), remote)
        assertEquals(setOf(attempt.activityId), restarted.observe("me").first().completed)
        assertEquals(1, restarted.observe("me").first().pending)
        remote.offline = false
        restarted.sync("me")
        restarted.sync("me")
        assertEquals(1, remote.rows.size)
        assertEquals(0, restarted.observe("me").first().pending)
        assertTrue(restarted.observe("other").first().attempts.isEmpty())
    }

    @Test fun replyAfterSignOutCannotRestorePrivateSnapshot() = runTest {
        val files = FakeFileStore()
        val gate = CompletableDeferred<Unit>()
        val remote = object : LearningRemote {
            override suspend fun fetch(memberId: String): List<LearningAttempt> { gate.await(); return listOf(attempt) }
            override suspend fun send(attempt: LearningAttempt) = Unit
        }
        val store = SnapshotStore(files)
        val repository = FileLearningRepository(store, remote)
        val job = launch { repository.sync("me") }
        runCurrent()
        repository.resetSession()
        store.clear()
        gate.complete(Unit)
        job.join()
        assertTrue(files.files.isEmpty())
        assertTrue(repository.observe("me").first().attempts.isEmpty())
    }
    @Test fun slowNetworkDoesNotBlockRecordingTheNextAttempt() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = object : LearningRemote {
            override suspend fun fetch(memberId: String): List<LearningAttempt> { gate.await(); return emptyList() }
            override suspend fun send(attempt: LearningAttempt) = Unit
        }
        val repository = FileLearningRepository(SnapshotStore(FakeFileStore()), remote)
        val syncing = launch { repository.sync("me") }
        runCurrent()
        repository.record(attempt)
        assertEquals(1, repository.observe("me").first().attempts.size)
        gate.complete(Unit)
        syncing.join()
        assertEquals(0, repository.observe("me").first().pending)
    }

    @Test fun historyWrittenBeforeTheStoredDtoIsStillRead() = runTest {
        val files = FakeFileStore()
        // The format files had when the domain model itself was serialized.
        files.writeText(
            "snapshots/learning-me.json",
            """[{"attempt":{"id":"a1","memberId":"me","activityId":"piano-chords-major","version":1,"passed":true,"day":"2026-10-18","hints":1},"synced":true}]""",
        )
        val progress = FileLearningRepository(SnapshotStore(files), Remote()).observe("me").first()
        assertEquals(listOf(attempt.copy(hints = 1)), progress.attempts)
        assertEquals(0, progress.pending)
    }

    @Test fun inTheDemoEverySyncSucceeds() = runTest {
        val repository = FileLearningRepository(SnapshotStore(FakeFileStore()), InMemoryLearningRemote())
        repository.record(attempt)
        assertEquals(1, repository.observe("me").first().pending)
        repository.sync("me")
        assertEquals(0, repository.observe("me").first().pending)
        assertEquals(listOf(attempt), repository.observe("me").first().attempts)
    }
}
