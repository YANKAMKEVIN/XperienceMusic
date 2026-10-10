package com.xperiencemusic.shared.presentation.app

import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.fakes.member
import kotlin.test.Test
import kotlin.test.assertEquals

class AppUiStateTest {
    @Test
    fun `chaque état de session mène au bon écran`() {
        assertEquals(AppUiState.SignIn, SessionState.SignedOut.toUiState())
        assertEquals(AppUiState.NotInvited("x@y.fr"), SessionState.NotInvited("x@y.fr").toUiState())
        assertEquals(AppUiState.Onboarding, SessionState.NeedsProfile("x@y.fr").toUiState())
        assertEquals(AppUiState.Unreachable, SessionState.Unreachable.toUiState())
        assertEquals(AppUiState.Ready, SessionState.Ready(member()).toUiState())
    }
}
