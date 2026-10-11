package com.xperiencemusic.shared.domain.model

/** How fresh the band's data on the phone is. */
enum class SyncStatus {
    /** Nothing attempted yet. */
    IDLE,
    SYNCING,
    UP_TO_DATE,

    /** The last attempt failed (usually no network); the copy on the phone is shown. */
    FAILED,
}
