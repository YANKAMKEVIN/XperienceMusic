package com.xperiencemusic.shared.domain.repository

/** Drops account-scoped in-memory data when a session ends. */
interface SessionResettable {
    fun resetSession()
}
