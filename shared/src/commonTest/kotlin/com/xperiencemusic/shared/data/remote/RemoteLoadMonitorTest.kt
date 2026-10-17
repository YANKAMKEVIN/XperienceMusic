package com.xperiencemusic.shared.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RemoteLoadMonitorTest {
    @Test
    fun failedLoadCanBeRetriedWithoutRestartingScreen() = runTest {
        val monitor = RemoteLoadMonitor()
        var offline = true
        var calls = 0
        monitor.load("planning") { calls++; check(!offline) }
        assertTrue(monitor.hasFailure.value)
        offline = false
        monitor.retry()
        assertFalse(monitor.hasFailure.value)
        assertEquals(2, calls)
    }

    @Test
    fun signingOutDropsPendingRetries() = runTest {
        val monitor = RemoteLoadMonitor()
        var calls = 0
        monitor.load("private") { calls++; error("offline") }
        monitor.clear()
        monitor.retry()
        assertEquals(1, calls)
        assertFalse(monitor.hasFailure.value)
    }

    @Test
    fun cancellationIsNeverPresentedAsNetworkFailure() = runTest {
        val monitor = RemoteLoadMonitor()
        assertFailsWith<CancellationException> {
            monitor.load("cancelled") { throw CancellationException() }
        }
        assertFalse(monitor.hasFailure.value)
    }
}
