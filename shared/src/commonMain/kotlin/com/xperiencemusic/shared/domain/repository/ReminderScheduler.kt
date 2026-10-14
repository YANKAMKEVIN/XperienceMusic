package com.xperiencemusic.shared.domain.repository

import com.xperiencemusic.shared.domain.model.Reminder

/** The phone's own notifications: no server involved, they fire even offline. */
interface ReminderScheduler {
    /** Replaces every reminder scheduled before with [reminders]. */
    suspend fun replaceAll(reminders: List<Reminder>)
}
