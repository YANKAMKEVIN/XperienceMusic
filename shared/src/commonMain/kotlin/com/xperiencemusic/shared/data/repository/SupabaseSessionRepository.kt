package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.data.local.SnapshotStore
import com.xperiencemusic.shared.data.mapper.toDomain
import com.xperiencemusic.shared.data.mapper.toUpdateDto
import com.xperiencemusic.shared.data.remote.dto.ProfileDto
import com.xperiencemusic.shared.data.remote.postgrestCall
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.repository.SessionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException

/**
 * Supabase Auth with email one-time codes. The member comes from `public.profiles`, which only
 * exists for invited emails (see the initial migration).
 *
 * Offline, a signed-in member is let in with the last profile saved on the phone, so songs and
 * downloaded recordings stay usable at church without network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseSessionRepository(
    private val client: SupabaseClient,
    private val snapshots: SnapshotStore,
    scope: CoroutineScope,
) : SessionRepository {
    /** Bumped to reload the profile after onboarding or a retry. */
    private val profileVersion = MutableStateFlow(0)

    private val state: StateFlow<SessionState> =
        combine(client.auth.sessionStatus, profileVersion) { status, _ -> status }
            .mapLatest { status ->
                when (status) {
                    SessionStatus.Initializing -> SessionState.Loading
                    is SessionStatus.NotAuthenticated -> SessionState.SignedOut
                    // The stored session could not be refreshed: most likely offline.
                    is SessionStatus.RefreshFailure -> offlineState()
                    is SessionStatus.Authenticated ->
                        status.session.user?.let { loadState(it) } ?: offlineState()
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)

    override fun observeSession(): Flow<SessionState> = state

    override suspend fun currentMember(): Member? = (state.value as? SessionState.Ready)?.member

    override suspend fun requestCode(email: String) {
        client.auth.signInWith(OTP) {
            this.email = email
            // Accounts are created on first sign-in; invitations decide who gets a profile.
            createUser = true
        }
    }

    override suspend fun verifyCode(email: String, code: String) {
        try {
            client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
        } catch (e: AuthRestException) {
            throw DomainException(DomainError.INVALID_CODE)
        }
    }

    override suspend fun saveProfile(profile: MemberProfile): Member {
        val userId = client.auth.currentUserOrNull()?.id ?: throw DomainException(DomainError.NOT_SIGNED_IN)
        val saved = postgrestCall {
            client.from(PROFILES).update(profile.toUpdateDto()) {
                select()
                filter { eq("id", userId) }
            }.decodeSingle<ProfileDto>()
        }
        snapshots.save(PROFILE_SNAPSHOT, ProfileDto.serializer(), saved)
        profileVersion.update { it + 1 }
        return saved.toDomain() ?: throw DomainException(DomainError.INVALID_INPUT)
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    override suspend fun refresh() {
        profileVersion.update { it + 1 }
    }

    private suspend fun loadState(user: UserInfo): SessionState {
        val email = user.email.orEmpty()
        return try {
            val profile = client.from(PROFILES)
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<ProfileDto>()
            profile?.let { snapshots.save(PROFILE_SNAPSHOT, ProfileDto.serializer(), it) }
            when {
                profile == null -> SessionState.NotInvited(email)
                else -> profile.toDomain()?.let { SessionState.Ready(it) } ?: SessionState.NeedsProfile(email)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            offlineState(user.id)
        }
    }

    /** The saved profile of [userId] (or of whoever last signed in on this phone) when offline. */
    private fun offlineState(userId: String? = client.auth.currentUserOrNull()?.id): SessionState {
        val cached = snapshots.load(PROFILE_SNAPSHOT, ProfileDto.serializer())
            ?.takeIf { userId == null || it.id == userId }
            ?.toDomain()
        return cached?.let { SessionState.Ready(it) } ?: SessionState.Unreachable
    }

    private companion object {
        const val PROFILES = "profiles"
        const val PROFILE_SNAPSHOT = "profile"
    }
}
