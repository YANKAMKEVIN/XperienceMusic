package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.fakes.FakeMicrophone
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackHoldNoteUseCaseTest {
    @Test
    fun `le silence ne compte pas et la bonne note finit par valider`() = runTest {
        val mic = FakeMicrophone(List(5) { null } + List(FakeMicrophone.framesFor(1.0)) { 57 })
        val progress = TrackHoldNoteUseCase(mic)(targetMidi = 57, requiredSeconds = 1.0).toList()

        val silent = progress.first()
        assertNull(silent.heardMidi)
        assertEquals(0.0, silent.holdSeconds)

        val singing = progress[10]
        assertEquals(57, singing.heardMidi)
        assertTrue(abs(singing.centsFromTarget!!) < 5)
        assertTrue(progress.last().isSuccess)
    }

    @Test
    fun `une autre note est entendue mais ne valide pas`() = runTest {
        val mic = FakeMicrophone(List(FakeMicrophone.framesFor(1.0)) { 59 })
        val last = TrackHoldNoteUseCase(mic)(targetMidi = 57, requiredSeconds = 1.0).toList().last()
        assertEquals(59, last.heardMidi)
        assertTrue(last.centsFromTarget!! > 150)
        assertFalse(last.isSuccess)
    }
}
