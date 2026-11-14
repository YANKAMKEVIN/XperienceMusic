package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.mapper.toBandMember
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.remote.RemoteLoadMonitor
import com.xperiencemusic.shared.data.remote.dto.InvitationDto
import com.xperiencemusic.shared.data.remote.dto.LevelUpdateDto
import com.xperiencemusic.shared.data.remote.dto.ProfileDto
import com.xperiencemusic.shared.data.remote.dto.RoleUpdateDto
import com.xperiencemusic.shared.data.remote.ignoringFailures
import com.xperiencemusic.shared.data.session.SessionGuard
import com.xperiencemusic.shared.data.session.accountCall
import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.Invitation
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.repository.MembersRepository
import com.xperiencemusic.shared.domain.repository.SessionResettable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart

/** Reads and writes `profiles` and `invitations`; the RLS policies restrict writes to super admins. */
class SupabaseMembersRepository(
    private val client: SupabaseClient,
    private val loads: RemoteLoadMonitor = RemoteLoadMonitor(),
) : MembersRepository, SessionResettable {
    private val session = SessionGuard()

    override fun resetSession() {
        session.reset()
        members.value = null
        invitations.value = null
    }

    private val members = MutableStateFlow<List<BandMember>?>(null)
    private val invitations = MutableStateFlow<List<Invitation>?>(null)

    override fun observeMembers(): Flow<List<BandMember>> =
        members.onStart { loads.load("Members") { reload() } }.filterNotNull()

    override fun observeInvitations(): Flow<List<Invitation>> =
        invitations.onStart { loads.load("Members") { reload() } }.filterNotNull()

    override suspend fun invite(email: String, role: Role) {
        session.accountCall { client.from(INVITATIONS).insert(InvitationDto(email, role.name)) }
        reload()
    }

    override suspend fun cancelInvitation(email: String) {
        session.accountCall { client.from(INVITATIONS).delete { filter { eq("email", email) } } }
        reload()
    }

    override suspend fun changeRole(member: BandMember, role: Role) {
        session.accountCall {
            client.from(PROFILES).update(RoleUpdateDto(role.name)) { filter { eq("id", member.id) } }
            // Kept in step so a removed-then-reinvited account gets the same role back.
            client.from(INVITATIONS).update(RoleUpdateDto(role.name)) { filter { eq("email", member.email) } }
        }
        reload()
    }

    override suspend fun changeLevel(member: BandMember, level: Level) {
        session.accountCall {
            client.from(PROFILES).update(LevelUpdateDto(level.name)) { filter { eq("id", member.id) } }
        }
        reload()
    }

    override suspend fun remove(member: BandMember) {
        session.accountCall {
            client.from(INVITATIONS).delete { filter { eq("email", member.email) } }
            client.from(PROFILES).delete { filter { eq("id", member.id) } }
        }
        reload()
    }

    private suspend fun reload() {
        val profiles = session.accountCall { client.from(PROFILES).select().decodeList<ProfileDto>() }
        val invites = session.accountCall { client.from(INVITATIONS).select().decodeList<InvitationDto>() }
        members.value = profiles.map { it.toBandMember() }
        invitations.value = invites.map { it.toDomain() }
    }

    private companion object {
        const val PROFILES = "profiles"
        const val INVITATIONS = "invitations"
    }
}
