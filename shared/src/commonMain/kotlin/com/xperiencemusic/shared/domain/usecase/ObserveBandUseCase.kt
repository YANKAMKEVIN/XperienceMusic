package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Band
import com.xperiencemusic.shared.domain.repository.MembersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Members by name (not yet onboarded last), and invitations nobody has used yet. */
class ObserveBandUseCase(private val members: MembersRepository) {
    operator fun invoke(): Flow<Band> =
        combine(members.observeMembers(), members.observeInvitations()) { all, invitations ->
            val memberEmails = all.map { it.email.lowercase() }.toSet()
            Band(
                members = all.sortedWith(compareBy({ it.displayName == null }, { (it.displayName ?: it.email).foldForSearch() })),
                pendingInvitations = invitations.filterNot { it.email in memberEmails }.sortedBy { it.email },
            )
        }
}
