package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.fakes.FakeFileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemorySessionRepositoryTest {
    private val files = FakeFileStore()
    private val profile = MemberProfile("Kevin", Instrument.SINGER, VoicePart.TENOR, Level.CONFIRMED)

    @Test
    fun `la session démo survit à un redémarrage`() = runTest {
        val first = InMemorySessionRepository(Role.CONTENT_ADMIN, files)
        first.verifyCode("kevin@example.com", InMemorySessionRepository.DEMO_CODE)
        // Closed between the code and the profile: the next run picks up at the profile.
        assertEquals(SessionState.NeedsProfile("kevin@example.com"), InMemorySessionRepository(files = files).observeSession().first())

        val member = first.saveProfile(profile)
        val restarted = InMemorySessionRepository(Role.SUPER_ADMIN, files)
        assertEquals(SessionState.Ready(member), restarted.observeSession().first())
        assertEquals(Role.CONTENT_ADMIN, restarted.currentMember()?.role)
    }

    @Test
    fun `se déconnecter efface la session enregistrée`() = runTest {
        val repository = InMemorySessionRepository(files = files)
        repository.verifyCode("kevin@example.com", InMemorySessionRepository.DEMO_CODE)
        repository.saveProfile(profile)
        repository.signOut()
        assertTrue(files.files.isEmpty())
        assertEquals(SessionState.SignedOut, InMemorySessionRepository(files = files).observeSession().first())
    }

    @Test
    fun `un fichier illisible ou sans stockage donne une session fermée`() = runTest {
        files.writeText("demo-session/session.json", "{pas du json")
        assertEquals(SessionState.SignedOut, InMemorySessionRepository(files = files).observeSession().first())
        assertEquals(SessionState.SignedOut, InMemorySessionRepository().observeSession().first())
    }
}
