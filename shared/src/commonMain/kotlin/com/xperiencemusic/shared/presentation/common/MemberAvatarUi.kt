package com.xperiencemusic.shared.presentation.common

import com.xperiencemusic.shared.domain.model.Avatar

/** What a member's circle needs: their avatar, or their initials while they have none. */
data class MemberAvatarUi(val name: String, val avatar: Avatar?) {
    /** "Grâce Mbala" → "GM", "Ruth" → "R". */
    val initials: String
        get() = name.trim().split(Regex("[\\s-]+")).filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercase() }
}
