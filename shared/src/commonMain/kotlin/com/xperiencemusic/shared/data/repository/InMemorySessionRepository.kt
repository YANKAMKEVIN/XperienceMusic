package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Prototype session until Supabase Auth is wired. The member who onboards on this device gets
 * [defaultRole]; with a real backend, roles come from the server and are granted by a super admin.
 */
class InMemorySessionRepository(
    private val defaultRole: Role = Role.SUPER_ADMIN,
) : SessionRepository {
    private val member = MutableStateFlow<Member?>(null)

    override fun observeCurrentMember(): Flow<Member?> = member.asStateFlow()

    override suspend fun currentMember(): Member? = member.value

    override suspend fun saveProfile(profile: MemberProfile): Member {
        val saved = Member(id = LOCAL_MEMBER_ID, profile = profile, role = member.value?.role ?: defaultRole)
        member.value = saved
        return saved
    }

    private companion object {
        const val LOCAL_MEMBER_ID = "local-member"
    }
}
