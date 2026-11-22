package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.HelpPage
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.model.helpAudiences
import com.xperiencemusic.shared.domain.repository.HelpRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The help pages of the signed-in member's role (see [helpAudiences]); a member's page while no one is. */
class ObserveHelpUseCase(private val sessions: SessionRepository, private val help: HelpRepository) {
    operator fun invoke(): Flow<List<HelpPage>> = sessions.observeSession()
        .map { (it as? SessionState.Ready)?.member?.role ?: Role.MEMBER }
        .distinctUntilChanged()
        .map { role -> role.helpAudiences().map(help::page) }
}
