package com.xperiencemusic.shared.domain.model

/** Someone with access to the app, onboarded or not yet. */
data class BandMember(
    val id: String,
    val email: String,
    /** Null until they answer the onboarding questions. */
    val displayName: String?,
    val role: Role,
    val instrument: Instrument?,
    val voicePart: VoicePart?,
    /** Null until onboarding. */
    val level: Level? = null,
)

/** An email allowed in, whose owner has not signed in yet. */
data class Invitation(val email: String, val role: Role)

data class Band(val members: List<BandMember>, val pendingInvitations: List<Invitation>)
