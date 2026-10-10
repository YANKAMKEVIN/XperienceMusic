package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.repository.SessionRepository

class CompleteOnboardingUseCase(private val sessions: SessionRepository) {
    suspend operator fun invoke(profile: MemberProfile): DomainResult<Member> {
        val name = profile.displayName.trim()
        if (name.isEmpty()) return DomainResult.Failure(DomainError.INVALID_INPUT)
        val cleaned = profile.copy(
            displayName = name,
            voicePart = profile.voicePart.takeIf { profile.instrument == Instrument.SINGER },
        )
        return runRemote { sessions.saveProfile(cleaned) }
    }
}
