package com.xperiencemusic.shared.presentation.common

import kotlin.test.Test
import kotlin.test.assertEquals

class SignedSemitonesTest {
    @Test
    fun `une transposition s'écrit avec son signe, un vrai moins`() {
        assertEquals("+2", signedSemitones(2))
        assertEquals("−1", signedSemitones(-1))
        assertEquals("0", signedSemitones(0))
    }
}
