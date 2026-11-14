package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.repository.AnnouncedMonthsStore
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException

/** What one pass of [SyncServiceRemindersUseCase] did. */
sealed interface ReminderSync {
    /** The phone now holds [reminders]; [announcedMonths] were told about for the first time. */
    data class Scheduled(val reminders: List<Reminder>, val announcedMonths: Set<LocalDate>) : ReminderSync

    /** Notifications refused or unavailable: nothing changed, the planning itself still works. */
    data object Unavailable : ReminderSync
}

/**
 * Keeps the phone's reminders in step with the planning and the setlists: each time either
 * changes, the reminders are planned again from scratch (see [planServiceReminders]). A month's
 * validation is announced once: it is remembered as announced as soon as it is scheduled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncServiceRemindersUseCase(
    private val observePlanning: ObservePlanningUseCase,
    private val observeSchedule: ObserveSetlistScheduleUseCase,
    private val scheduler: ReminderScheduler,
    private val announced: AnnouncedMonthsStore,
    private val now: () -> LocalDateTime,
) {
    operator fun invoke(): Flow<ReminderSync> =
        combine(observePlanning(), observeSchedule(), ::Pair).mapLatest { (planning, schedule) ->
            val reminders = planServiceReminders(planning, schedule, ReminderContext(now(), announced.load()))
            try {
                scheduler.replaceAll(reminders)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@mapLatest ReminderSync.Unavailable
            }
            val months = reminders.filterIsInstance<Reminder.MonthValidated>().map { it.month }.toSet()
            if (months.isNotEmpty()) announced.save(announced.load() + months)
            ReminderSync.Scheduled(reminders, months)
        }
}
