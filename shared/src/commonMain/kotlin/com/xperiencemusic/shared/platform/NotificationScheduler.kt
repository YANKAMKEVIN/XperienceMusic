package com.xperiencemusic.shared.platform

import kotlinx.datetime.LocalDateTime

/** A notification as the phone shows it, words included. */
data class LocalNotification(
    val id: String,
    val at: LocalDateTime,
    val title: String,
    val body: String,
)

/** The phone's own notifications: no server involved, they fire even offline. */
interface NotificationScheduler {
    /** Replaces every notification scheduled before with [notifications]; those already due show at once. */
    suspend fun replaceAll(notifications: List<LocalNotification>)
}
