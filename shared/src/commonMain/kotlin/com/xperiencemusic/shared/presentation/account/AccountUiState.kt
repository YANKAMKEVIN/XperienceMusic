package com.xperiencemusic.shared.presentation.account

import com.xperiencemusic.shared.domain.model.AvatarPreset
import com.xperiencemusic.shared.domain.model.Instrument
import com.xperiencemusic.shared.domain.model.Level
import com.xperiencemusic.shared.domain.model.Member
import com.xperiencemusic.shared.domain.model.Permission
import com.xperiencemusic.shared.domain.model.Role
import com.xperiencemusic.shared.domain.model.VoicePart
import com.xperiencemusic.shared.presentation.common.MemberAvatarUi

/** "Mon compte": who the member is, their avatar, and the tools their role opens. */
data class AccountUiState(
    val isLoading: Boolean = true,
    val me: MemberAvatarUi = MemberAvatarUi("", null),
    val instrument: Instrument? = null,
    val voicePart: VoicePart? = null,
    val level: Level? = null,
    val role: Role? = null,
    /** Their own role's avatars first. */
    val presets: List<AvatarPreset> = emptyList(),
    val canReview: Boolean = false,
    val toReviewCount: Int = 0,
    val toValidateCount: Int = 0,
    val canManageMembers: Boolean = false,
    /** A new avatar is being sent. */
    val isSaving: Boolean = false,
    val error: AccountError? = null,
) {
    val hasAvatar: Boolean get() = me.avatar != null
}

enum class AccountError { SAVE_FAILED, PHOTO_UNREADABLE }

sealed interface AccountEvent {
    data class PresetChosen(val preset: AvatarPreset) : AccountEvent

    /** Null when the picker gave nothing usable back (not a cancel: those send no event). */
    data class PhotoPicked(val jpeg: ByteArray?) : AccountEvent {
        override fun equals(other: Any?) = other is PhotoPicked && other.jpeg.contentEquals(jpeg)
        override fun hashCode() = jpeg.contentHashCode()
    }

    data object RemoveAvatarClicked : AccountEvent
    data object ErrorShown : AccountEvent
}

fun deriveAccountState(member: Member?): AccountUiState {
    if (member == null) return AccountUiState()
    val profile = member.profile
    return AccountUiState(
        isLoading = false,
        me = MemberAvatarUi(profile.displayName, member.avatar),
        instrument = profile.instrument,
        voicePart = profile.voicePart.takeIf { profile.instrument == Instrument.SINGER },
        level = profile.level,
        role = member.role,
        presets = AvatarPreset.orderedFor(profile.instrument),
        canReview = member.role.can(Permission.REVIEW_TRAINING),
        canManageMembers = member.role.can(Permission.MANAGE_MEMBERS),
    )
}
