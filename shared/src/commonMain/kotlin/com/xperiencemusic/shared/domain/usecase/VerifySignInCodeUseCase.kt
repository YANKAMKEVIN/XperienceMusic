package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.SessionRepository

class VerifySignInCodeUseCase(private val sessions: SessionRepository) {
    suspend operator fun invoke(email: String, code: String): DomainResult<Unit> {
        val digits = code.filter { it.isDigit() }
        if (digits.length != CODE_LENGTH) return DomainResult.Failure(DomainError.INVALID_CODE)
        return runRemote { sessions.verifyCode(email, digits) }
    }

    companion object {
        const val CODE_LENGTH = 6
    }
}
