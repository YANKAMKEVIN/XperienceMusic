package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.repository.SessionRepository

/**
 * "Mon profil": the member corrects their first name, role and voice. The level is not theirs to
 * change once set (the server's `guard_profile_update` refuses it): the current one is sent back.
 */
class UpdateMyProfileUseCase(private val sessions: SessionRepository) {
    suspend operator fun invoke(name: String, instrument: Instrument, voicePart: VoicePart?): DomainResult<Member> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return DomainResult.Failure(DomainError.INVALID_INPUT)
        val current = sessions.currentMember() ?: return DomainResult.Failure(DomainError.NOT_SIGNED_IN)
        val profile = current.profile.copy(
            displayName = trimmed,
            instrument = instrument,
            voicePart = voicePart.takeIf { instrument == Instrument.SINGER },
        )
        return runRemote { sessions.saveProfile(profile) }
    }
}
