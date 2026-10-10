package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** The signed-in member, or null before onboarding. */
    fun observeCurrentMember(): Flow<Member?>

    suspend fun currentMember(): Member?

    suspend fun saveProfile(profile: MemberProfile): Member
}
