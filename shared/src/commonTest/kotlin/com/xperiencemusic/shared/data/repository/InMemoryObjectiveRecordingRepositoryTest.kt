package com.xperiencemusic.shared.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryObjectiveRecordingRepositoryTest {
    @Test
    fun newAttemptsPreserveHistoryAndKeepMembersSeparate() = runTest {
        val storage = InMemoryAudioStorage()
        val repository = InMemoryObjectiveRecordingRepository(storage)
        repository.send("me", "piano-c2", byteArrayOf(1), 4)
        val first = repository.observe("me").first().single().audioPath
        repository.send("me", "piano-c2", byteArrayOf(2), 5)
        assertEquals(2, storage.paths.size)
        assertTrue(first in storage.paths)
        assertEquals(5, repository.observe("me").first().last().seconds)
        assertTrue(repository.observe("other").first().isEmpty())
        repository.resetSession()
        assertTrue(repository.observe("me").first().isEmpty())
    }
}
