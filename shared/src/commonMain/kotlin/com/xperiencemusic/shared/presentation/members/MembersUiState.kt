package com.xperiencemusic.shared.presentation.members

import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.VoicePart

data class MembersUiState(
    val isLoading: Boolean = true,
    val inviteEmail: String = "",
    val inviteRole: Role = Role.MEMBER,
    val isWorking: Boolean = false,
    val members: List<MemberRowUi> = emptyList(),
    val pendingInvitations: List<InvitationRowUi> = emptyList(),
    val pendingRemoval: MemberRowUi? = null,
    val message: MembersMessage? = null,
) {
    val canInvite: Boolean get() = !isWorking && inviteEmail.isNotBlank()
}

data class MemberRowUi(
    val id: String,
    /** Display name, or the email until they finish onboarding. */
    val name: String,
    val email: String,
    val hasOnboarded: Boolean,
    val role: Role,
    val instrument: Instrument?,
    val voicePart: VoicePart?,
    val isMe: Boolean,
)

data class InvitationRowUi(val email: String, val role: Role)

enum class MembersMessage { INVITED, ALREADY_INVITED, INVALID_EMAIL, CANNOT_CHANGE_SELF, FORBIDDEN, NETWORK }

sealed interface MembersEvent {
    data class InviteEmailChanged(val email: String) : MembersEvent
    data class InviteRoleSelected(val role: Role) : MembersEvent
    data object InviteClicked : MembersEvent
    data class RoleChanged(val memberId: String, val role: Role) : MembersEvent
    data class RemoveClicked(val memberId: String) : MembersEvent
    data object RemoveConfirmed : MembersEvent
    data object RemoveDismissed : MembersEvent
    data class CancelInvitationClicked(val email: String) : MembersEvent
    data object MessageShown : MembersEvent
}
