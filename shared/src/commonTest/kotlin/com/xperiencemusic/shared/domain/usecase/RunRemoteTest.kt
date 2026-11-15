package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.ErrorLog
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.DomainResult
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RunRemoteTest {
    private val logged = mutableListOf<Throwable>()
    private val defaultSink = ErrorLog.sink

    @BeforeTest
    fun captureLog() {
        ErrorLog.sink = { _, cause -> logged += cause }
    }

    @AfterTest
    fun restoreLog() {
        ErrorLog.sink = defaultSink
    }

    @Test
    fun `an I-O failure is a network error`() = runTest {
        val result = runRemote<Unit> { throw IOException("offline") }

        assertEquals(DomainResult.Failure(DomainError.NETWORK), result)
        assertTrue(logged.isEmpty())
    }

    @Test
    fun `an unexpected failure is an unknown error, not a network one, and its cause is logged`() = runTest {
        val bug = IllegalStateException("decoding failed")

        val result = runRemote<Unit> { throw bug }

        assertEquals(DomainResult.Failure(DomainError.UNKNOWN), result)
        assertSame(bug, logged.single())
    }

    @Test
    fun `a domain failure keeps its error`() = runTest {
        val result = runRemote<Unit> { throw DomainException(DomainError.FORBIDDEN) }

        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), result)
    }
}
