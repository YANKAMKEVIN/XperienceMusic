package com.xperiencemusic.shared.presentation.account

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.fakes.member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountStateTest {
    @Test
    fun `un membre voit son profil sans les outils du responsable`() {
        val state = deriveAccountState(member(instrument = Instrument.BASS, voicePart = null))
        assertFalse(state.isLoading)
        assertEquals("Grâce", state.me.name)
        assertFalse(state.canReview || state.canManageMembers)
        assertFalse(state.hasAvatar)
        assertEquals(Instrument.BASS, state.presets.first().instrument)
    }

    @Test
    fun `la voix ne s'affiche que pour les chantres`() {
        assertNull(deriveAccountState(member(instrument = Instrument.PIANO)).voicePart)
    }

    @Test
    fun `l'Admin++ retrouve la relecture et les membres`() {
        val state = deriveAccountState(member(role = Role.SUPER_ADMIN))
        assertTrue(state.canReview && state.canManageMembers)
    }
}
