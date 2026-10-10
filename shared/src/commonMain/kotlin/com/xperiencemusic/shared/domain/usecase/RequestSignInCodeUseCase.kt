package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.SessionRepository

class RequestSignInCodeUseCase(private val sessions: SessionRepository) {
    /** Returns the normalized email the code was sent to. */
    suspend operator fun invoke(email: String): DomainResult<String> {
        val normalized = email.trim().lowercase()
        if (!EMAIL.matches(normalized)) return DomainResult.Failure(DomainError.INVALID_INPUT)
        return runRemote {
            sessions.requestCode(normalized)
            normalized
        }
    }

    private companion object {
        val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
    }
}
