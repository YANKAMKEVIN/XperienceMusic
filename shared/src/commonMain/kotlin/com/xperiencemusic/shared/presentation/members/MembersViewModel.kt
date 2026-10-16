package com.xperiencemusic.shared.presentation.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.model.Band
import com.xperiencemusic.shared.domain.model.BandMember
import com.xperiencemusic.shared.domain.model.DomainError
import com.xperiencemusic.shared.domain.model.DomainResult
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.usecase.CancelInvitationUseCase
import com.xperiencemusic.shared.domain.usecase.ChangeMemberLevelUseCase
import com.xperiencemusic.shared.domain.usecase.ChangeMemberRoleUseCase
import com.xperiencemusic.shared.domain.usecase.InviteMemberUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveBandUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveCurrentMemberUseCase
import com.xperiencemusic.shared.domain.usecase.RemoveMemberUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Screen-local state of the members screen. */
data class MembersForm(
    val inviteEmail: String = "",
    val inviteRole: Role = Role.MEMBER,
    val isWorking: Boolean = false,
    val pendingRemovalId: String? = null,
    val message: MembersMessage? = null,
)

class MembersViewModel(
    observeBand: ObserveBandUseCase,
    observeCurrentMember: ObserveCurrentMemberUseCase,
    private val inviteMember: InviteMemberUseCase,
    private val changeMemberRole: ChangeMemberRoleUseCase,
    private val changeMemberLevel: ChangeMemberLevelUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val cancelInvitation: CancelInvitationUseCase,
) : ViewModel() {
    private val form = MutableStateFlow(MembersForm())

    val state: StateFlow<MembersUiState> =
        combine(observeBand(), observeCurrentMember(), form, ::deriveMembersState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MembersUiState())

    fun onEvent(event: MembersEvent) {
        when (event) {
            is MembersEvent.InviteEmailChanged -> form.update { it.copy(inviteEmail = event.email) }
            is MembersEvent.InviteRoleSelected -> form.update { it.copy(inviteRole = event.role) }
            MembersEvent.InviteClicked -> invite()
            is MembersEvent.RoleChanged -> memberOf(event.memberId)?.let { member ->
                perform { changeMemberRole(member, event.role) }
            }
            is MembersEvent.LevelChanged -> memberOf(event.memberId)?.let { member ->
                perform { changeMemberLevel(member, event.level) }
            }
            is MembersEvent.RemoveClicked -> form.update { it.copy(pendingRemovalId = event.memberId) }
            MembersEvent.RemoveDismissed -> form.update { it.copy(pendingRemovalId = null) }
            MembersEvent.RemoveConfirmed -> {
                val member = form.value.pendingRemovalId?.let(::memberOf)
                form.update { it.copy(pendingRemovalId = null) }
                member?.let { perform { removeMember(it) } }
            }
            is MembersEvent.CancelInvitationClicked -> perform { cancelInvitation(event.email) }
            MembersEvent.MessageShown -> form.update { it.copy(message = null) }
        }
    }

    private fun invite() {
        val current = form.value
        perform(onSuccess = { it.copy(inviteEmail = "", inviteRole = Role.MEMBER, message = MembersMessage.INVITED) }) {
            inviteMember(current.inviteEmail, current.inviteRole)
        }
    }

    private fun memberOf(id: String): BandMember? = state.value.members.firstOrNull { it.id == id }?.let {
        BandMember(it.id, it.email, it.name.takeIf { _ -> it.hasOnboarded }, it.role, it.instrument, it.voicePart, it.level)
    }

    private fun perform(onSuccess: (MembersForm) -> MembersForm = { it }, action: suspend () -> DomainResult<*>) {
        form.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            val result = action()
            form.update {
                val done = it.copy(isWorking = false)
                when (result) {
                    is DomainResult.Success -> onSuccess(done)
                    is DomainResult.Failure -> done.copy(message = result.error.toMessage())
                }
            }
        }
    }
}

fun deriveMembersState(band: Band, me: Member?, form: MembersForm): MembersUiState {
    val rows = band.members.map {
        MemberRowUi(
            id = it.id,
            name = it.displayName ?: it.email,
            email = it.email,
            hasOnboarded = it.displayName != null,
            role = it.role,
            instrument = it.instrument,
            voicePart = it.voicePart,
            isMe = it.id == me?.id,
            level = it.level,
        )
    }
    return MembersUiState(
        isLoading = false,
        inviteEmail = form.inviteEmail,
        inviteRole = form.inviteRole,
        isWorking = form.isWorking,
        members = rows,
        pendingInvitations = band.pendingInvitations.map { InvitationRowUi(it.email, it.role) },
        pendingRemoval = rows.firstOrNull { it.id == form.pendingRemovalId },
        message = form.message,
    )
}

private fun DomainError.toMessage() = when (this) {
    DomainError.INVALID_INPUT -> MembersMessage.INVALID_EMAIL
    DomainError.ALREADY_INVITED -> MembersMessage.ALREADY_INVITED
    DomainError.CANNOT_CHANGE_SELF -> MembersMessage.CANNOT_CHANGE_SELF
    DomainError.FORBIDDEN, DomainError.NOT_SIGNED_IN -> MembersMessage.FORBIDDEN
    else -> MembersMessage.NETWORK
}
