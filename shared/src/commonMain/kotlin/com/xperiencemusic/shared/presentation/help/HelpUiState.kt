package com.xperiencemusic.shared.presentation.help

import com.xperiencemusic.shared.domain.model.HelpAudience
import com.xperiencemusic.shared.domain.model.HelpPage

/** "Aide": the pages of the member's role, one shown at a time. */
data class HelpUiState(
    val isLoading: Boolean = true,
    /** The tabs, in [com.xperiencemusic.shared.domain.model.helpAudiences] order; none for a member. */
    val audiences: List<HelpAudience> = emptyList(),
    val selected: HelpAudience? = null,
    val page: HelpPage? = null,
) {
    val showsTabs: Boolean get() = audiences.size > 1
}

sealed interface HelpEvent {
    data class AudienceSelected(val audience: HelpAudience) : HelpEvent
}

/** Opens on the role's first page; a choice the role cannot read (after a role change) falls back to it. */
fun deriveHelpState(pages: List<HelpPage>, chosen: HelpAudience?): HelpUiState {
    val page = pages.firstOrNull { it.audience == chosen } ?: pages.firstOrNull() ?: return HelpUiState()
    val audiences = pages.map { it.audience }
    return HelpUiState(
        isLoading = false,
        audiences = audiences.takeIf { it.size > 1 }.orEmpty(),
        selected = page.audience,
        page = page,
    )
}
