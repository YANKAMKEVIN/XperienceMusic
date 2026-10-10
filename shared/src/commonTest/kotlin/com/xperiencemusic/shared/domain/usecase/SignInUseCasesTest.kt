package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SignInUseCasesTest {
    private val sessions = FakeSessionRepository()

    @Test
    fun `l'e-mail est normalisé avant l'envoi du code`() = runTest {
        assertEquals(DomainResult.Success("grace@band.fr"), RequestSignInCodeUseCase(sessions)("  Grace@Band.FR "))
        assertEquals(listOf("grace@band.fr"), sessions.requestedCodes)
    }

    @Test
    fun `un e-mail invalide n'appelle pas le serveur`() = runTest {
        assertEquals(DomainResult.Failure(DomainError.INVALID_INPUT), RequestSignInCodeUseCase(sessions)("grace"))
        assertEquals(emptyList(), sessions.requestedCodes)
    }

    @Test
    fun `une panne réseau devient une erreur NETWORK`() = runTest {
        sessions.failure = IllegalStateException("offline")
        assertEquals(DomainResult.Failure(DomainError.NETWORK), RequestSignInCodeUseCase(sessions)("grace@band.fr"))
    }

    @Test
    fun `un refus métier du serveur garde son erreur`() = runTest {
        sessions.failure = DomainException(DomainError.FORBIDDEN)
        assertEquals(DomainResult.Failure(DomainError.FORBIDDEN), RequestSignInCodeUseCase(sessions)("grace@band.fr"))
    }

    @Test
    fun `le code doit faire 6 chiffres, espaces tolérés`() = runTest {
        val verify = VerifySignInCodeUseCase(sessions)
        assertEquals(DomainResult.Failure(DomainError.INVALID_CODE), verify("g@b.fr", "12345"))
        assertEquals(DomainResult.Success(Unit), verify("g@b.fr", "123 456"))
        assertEquals(SessionState.NeedsProfile("g@b.fr"), sessions.state.value)
    }

    @Test
    fun `un mauvais code est signalé`() = runTest {
        assertEquals(DomainResult.Failure(DomainError.INVALID_CODE), VerifySignInCodeUseCase(sessions)("g@b.fr", "000000"))
    }
}
