package com.xperiencemusic.shared.fakes

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.repository.AnnouncedMonthsStore
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import kotlinx.datetime.LocalDate

/** Keeps the last reminders it was given; [isUnavailable] makes it fail like refused notifications. */
class FakeReminderScheduler(var isUnavailable: Boolean = false) : ReminderScheduler {
    var scheduled: List<Reminder>? = null
        private set

    override suspend fun replaceAll(reminders: List<Reminder>) {
        if (isUnavailable) error("notifications refused")
        scheduled = reminders
    }
}

class FakeAnnouncedMonthsStore(var months: Set<LocalDate> = emptySet()) : AnnouncedMonthsStore {
    override fun load(): Set<LocalDate> = months

    override fun save(months: Set<LocalDate>) {
        this.months = months
    }
}
