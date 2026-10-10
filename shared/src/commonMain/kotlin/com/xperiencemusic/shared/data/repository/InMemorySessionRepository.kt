package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Offline demo session, used when no Supabase project is configured. Any email works and the code
 * is always [DEMO_CODE]; the member gets [defaultRole].
 */
class InMemorySessionRepository(
    private val defaultRole: Role = Role.SUPER_ADMIN,
) : SessionRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.SignedOut)

    override fun observeSession(): Flow<SessionState> = state.asStateFlow()

    override suspend fun currentMember(): Member? = (state.value as? SessionState.Ready)?.member

    override suspend fun requestCode(email: String) = Unit

    override suspend fun verifyCode(email: String, code: String) {
        if (code != DEMO_CODE) throw DomainException(DomainError.INVALID_CODE)
        state.value = SessionState.NeedsProfile(email)
    }

    override suspend fun saveProfile(profile: MemberProfile): Member {
        val role = currentMember()?.role ?: defaultRole
        val member = Member(id = LOCAL_MEMBER_ID, profile = profile, role = role)
        state.value = SessionState.Ready(member)
        return member
    }

    override suspend fun signOut() {
        state.value = SessionState.SignedOut
    }

    override suspend fun refresh() = Unit

    companion object {
        const val DEMO_CODE = "123456"
        private const val LOCAL_MEMBER_ID = "local-member"
    }
}
