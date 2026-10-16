package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Invitation
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/** Demo band: the person using the app plus a few fictional members. */
class InMemoryMembersRepository(private val sessions: SessionRepository) : MembersRepository {
    private val others = MutableStateFlow(
        listOf(
            BandMember("demo-grace", "grace@exemple.fr", "Grâce", Role.MEMBER, Instrument.SINGER, VoicePart.ALTO, Level.BEGINNER),
            BandMember("demo-daniel", "daniel@exemple.fr", "Daniel", Role.CONTENT_ADMIN, Instrument.PIANO, null, Level.BEGINNER),
            BandMember("demo-samuel", "samuel@exemple.fr", null, Role.MEMBER, null, null),
        ),
    )
    private val invitations = MutableStateFlow(
        others.value.map { Invitation(it.email, it.role) } + Invitation("ruth@exemple.fr", Role.MEMBER),
    )

    override fun observeMembers(): Flow<List<BandMember>> =
        combine(others, sessions.observeSession()) { list, session ->
            val me = (session as? SessionState.Ready)?.member?.let {
                BandMember(it.id, DEMO_EMAIL, it.profile.displayName, it.role, it.profile.instrument, it.profile.voicePart, it.profile.level)
            }
            listOfNotNull(me) + list
        }

    override fun observeInvitations(): Flow<List<Invitation>> = invitations

    override suspend fun invite(email: String, role: Role) {
        invitations.update { it + Invitation(email, role) }
    }

    override suspend fun cancelInvitation(email: String) {
        invitations.update { list -> list.filterNot { it.email == email } }
    }

    override suspend fun changeRole(member: BandMember, role: Role) {
        others.update { list -> list.map { if (it.id == member.id) it.copy(role = role) else it } }
    }

    override suspend fun changeLevel(member: BandMember, level: Level) {
        others.update { list -> list.map { if (it.id == member.id) it.copy(level = level) else it } }
    }

    override suspend fun remove(member: BandMember) {
        others.update { list -> list.filterNot { it.id == member.id } }
        cancelInvitation(member.email)
    }

    private companion object {
        const val DEMO_EMAIL = "toi@demo.local"
    }
}
