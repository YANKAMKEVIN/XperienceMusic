package com.xperiencemusic.shared.data.repository

import com.xperiencemusic.shared.domain.model.Avatar
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.decodeAvatar
import com.xperiencemusic.shared.domain.model.encode
import com.xperiencemusic.shared.domain.model.DomainException
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.MemberProfile
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.SessionState
import com.xperiencemusic.shared.domain.repository.SessionRepository
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.domain.repository.FileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Offline demo session, used when no Supabase project is configured. Any email works and the code
 * is always [DEMO_CODE]; the member gets [defaultRole]. With a [files] store the sign-in and the
 * profile survive an app restart (demos and trials); everything else in the demo stays in memory.
 */
class InMemorySessionRepository(
    private val defaultRole: Role = Role.SUPER_ADMIN,
    private val files: FileStore? = null,
) : SessionRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val state = MutableStateFlow(restore())

    override fun observeSession(): Flow<SessionState> = state.asStateFlow()

    override suspend fun currentMember(): Member? = (state.value as? SessionState.Ready)?.member

    override suspend fun requestCode(email: String) = Unit

    override suspend fun verifyCode(email: String, code: String) {
        if (code != DEMO_CODE) throw DomainException(DomainError.INVALID_CODE)
        state.value = SessionState.NeedsProfile(email)
        persist()
    }

    override suspend fun saveProfile(profile: MemberProfile): Member {
        val current = currentMember()
        val member = Member(id = LOCAL_MEMBER_ID, profile = profile, role = current?.role ?: defaultRole, avatar = current?.avatar)
        state.value = SessionState.Ready(member)
        persist()
        return member
    }

    override suspend fun saveAvatar(avatar: Avatar?): Member {
        val member = (currentMember() ?: throw DomainException(DomainError.NOT_SIGNED_IN)).copy(avatar = avatar)
        state.value = SessionState.Ready(member)
        persist()
        return member
    }

    override suspend fun signOut() {
        state.value = SessionState.SignedOut
        files?.delete(DIRECTORY)
    }

    override suspend fun refresh() = Unit

    private fun persist() {
        val saved = when (val current = state.value) {
            is SessionState.NeedsProfile -> SavedSession(email = current.email)
            is SessionState.Ready -> with(current.member) {
                SavedSession(id = id, displayName = profile.displayName, instrument = profile.instrument,
                    voicePart = profile.voicePart, level = profile.level, role = role,
                    // A demo photo lives in memory only: it would point at nothing after a restart.
                    avatar = (avatar as? Avatar.Preset)?.encode())
            }
            else -> null
        } ?: return
        files?.writeText(FILE, json.encodeToString(saved))
    }

    /** The session of the previous run, or signed out when there is none (or it cannot be read). */
    private fun restore(): SessionState {
        val saved = files?.readText(FILE)
            ?.let { runCatching { json.decodeFromString<SavedSession>(it) }.getOrNull() }
            ?: return SessionState.SignedOut
        if (saved.email != null) return SessionState.NeedsProfile(saved.email)
        val profile = MemberProfile(
            displayName = saved.displayName ?: return SessionState.SignedOut,
            instrument = saved.instrument ?: return SessionState.SignedOut,
            voicePart = saved.voicePart,
            level = saved.level ?: return SessionState.SignedOut,
        )
        return SessionState.Ready(Member(saved.id ?: LOCAL_MEMBER_ID, profile, saved.role ?: defaultRole, decodeAvatar(saved.avatar)))
    }

    @Serializable
    private data class SavedSession(
        val email: String? = null,
        val id: String? = null,
        val displayName: String? = null,
        val instrument: Instrument? = null,
        val voicePart: VoicePart? = null,
        val level: Level? = null,
        val role: Role? = null,
        val avatar: String? = null,
    )

    companion object {
        const val DIRECTORY = "demo-session"
        private const val FILE = "$DIRECTORY/session.json"

        const val DEMO_CODE = "123456"
        private const val LOCAL_MEMBER_ID = "local-member"
    }
}
