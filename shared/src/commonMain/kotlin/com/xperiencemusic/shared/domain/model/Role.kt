package com.xperiencemusic.shared.domain.model

/** What a member is allowed to do. Roles are granted by a [SUPER_ADMIN], never chosen at onboarding. */
enum class Role(val permissions: Set<Permission>) {
    /** Reads everything, practises, submits training recordings. */
    MEMBER(emptySet()),

    /** Manages the shared content: setlists, lyrics, chords, recordings, and who serves when. */
    CONTENT_ADMIN(
        setOf(
            Permission.PUBLISH_SETLIST,
            Permission.PLAN_SERVICES,
            Permission.EDIT_SONG,
            Permission.MANAGE_RECORDINGS,
        ),
    ),

    /** Everything a content admin does, plus reviewing singers' training and managing members. */
    SUPER_ADMIN(
        CONTENT_ADMIN.permissions + setOf(
            Permission.REVIEW_TRAINING,
            Permission.MANAGE_MEMBERS,
        ),
    ),
    ;

    fun can(permission: Permission): Boolean = permission in permissions
}

enum class Permission {
    PUBLISH_SETLIST,
    PLAN_SERVICES,
    EDIT_SONG,
    MANAGE_RECORDINGS,
    REVIEW_TRAINING,
    MANAGE_MEMBERS,
}
