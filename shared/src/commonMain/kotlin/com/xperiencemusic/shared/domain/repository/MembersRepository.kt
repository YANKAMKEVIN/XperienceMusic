package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.Invitation
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Role
import kotlinx.coroutines.flow.Flow

/** Who may use the app. Server-side, only a super admin can change any of it. */
interface MembersRepository {
    fun observeMembers(): Flow<List<BandMember>>

    /** Every invitation, accepted or not. */
    fun observeInvitations(): Flow<List<Invitation>>

    suspend fun invite(email: String, role: Role)

    suspend fun cancelInvitation(email: String)

    suspend fun changeRole(member: BandMember, role: Role)

    suspend fun changeLevel(member: BandMember, level: Level)

    /** Removes access: the account stays, but without a profile it sees nothing. */
    suspend fun remove(member: BandMember)
}
