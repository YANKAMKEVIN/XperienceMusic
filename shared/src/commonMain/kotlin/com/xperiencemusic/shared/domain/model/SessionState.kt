package com.xperiencemusic.shared.domain.model

/** Where the person using the app stands, from signed out to fully set up. */
sealed interface SessionState {
    data object Loading : SessionState

    data object SignedOut : SessionState

    /** Signed in, but the member's profile could not be loaded (offline, server down…). */
    data object Unreachable : SessionState

    /** Signed in with an email nobody invited into the band. */
    data class NotInvited(val email: String) : SessionState

    /** Invited and signed in, but the onboarding questions are not answered yet. */
    data class NeedsProfile(val email: String) : SessionState

    data class Ready(val member: Member) : SessionState
}
