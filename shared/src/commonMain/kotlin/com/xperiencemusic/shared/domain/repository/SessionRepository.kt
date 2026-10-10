package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

/** Passwordless sign-in: a one-time code is emailed, then typed back in the app. */
interface SessionRepository {
    fun observeSession(): Flow<SessionState>

    /** The member once [SessionState.Ready], null otherwise. */
    suspend fun currentMember(): Member?

    suspend fun requestCode(email: String)

    /** Throws when the code is wrong or expired. */
    suspend fun verifyCode(email: String, code: String)

    suspend fun saveProfile(profile: MemberProfile): Member

    suspend fun signOut()

    /** Tries again to load the member after [com.xperiencemusic.shared.domain.model.SessionState.Unreachable]. */
    suspend fun refresh()
}
