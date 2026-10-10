package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.repository.LocalDataStore
import com.xperiencemusic.shared.domain.repository.SessionRepository

/** Signs out and wipes what the phone kept about the band (copies of data, downloaded audio). */
class SignOutUseCase(
    private val sessions: SessionRepository,
    private val localData: LocalDataStore,
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        // Wiped even if the server cannot be told: the phone may be handed to someone else.
        localData.clearAll()
        return runRemote { sessions.signOut() }
    }
}
