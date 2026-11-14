package com.xperiencemusic.shared.data.session

import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionGuardTest {
    @Test
    fun callReturnsTheResultWhileTheSessionLasts() = runTest {
        val guard = SessionGuard()

        assertEquals(42, guard.call { 42 })
    }

    @Test
    fun callDropsAResultThatArrivesAfterTheSessionEnded() = runTest {
        val guard = SessionGuard()

        val error = assertFailsWith<CancellationException> {
            guard.call {
                guard.reset()
                "previous account's rows"
            }
        }
        assertEquals("Session ended", error.message)
    }

    @Test
    fun aResetOnlyOutdatesTheWorkStartedBeforeIt() = runTest {
        val guard = SessionGuard()
        val before = guard.stamp()

        guard.reset()
        val after = guard.stamp()

        assertFalse(guard.isCurrent(before))
        assertTrue(guard.isCurrent(after))
        assertFailsWith<CancellationException> { guard.ensureCurrent(before) }
        guard.ensureCurrent(after)
        assertEquals("next account", guard.call { "next account" })
    }

    @Test
    fun failuresOfTheBlockPassThrough() = runTest {
        val guard = SessionGuard()

        assertFailsWith<IllegalStateException> { guard.call { error("offline") } }
    }
}
