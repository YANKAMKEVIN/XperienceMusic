package com.xperiencemusic.shared.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xperiencemusic.shared.domain.repository.AnnouncedMonthsStore
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import com.xperiencemusic.shared.domain.usecase.ObservePlanningUseCase
import com.xperiencemusic.shared.domain.usecase.ObserveSetlistScheduleUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Lives as long as the signed-in shell: each time the planning or the setlists change, the phone's
 * reminders are planned again from scratch.
 */
class RemindersViewModel(
    observePlanning: ObservePlanningUseCase,
    observeSchedule: ObserveSetlistScheduleUseCase,
    private val scheduler: ReminderScheduler,
    private val announced: AnnouncedMonthsStore,
    private val now: () -> LocalDateTime,
) : ViewModel() {
    init {
        viewModelScope.launch {
            combine(observePlanning(), observeSchedule(), ::Pair).collectLatest { (planning, schedule) ->
                val reminders = planServiceReminders(planning, schedule, ReminderContext(now(), announced.load()))
                try {
                    scheduler.replaceAll(reminders)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Notifications refused or unavailable: the planning itself still works.
                    return@collectLatest
                }
                // A validation is announced once: it is shown now and left out of later plans.
                val months = reminders.filter { it.id.startsWith(ANNOUNCEMENT) }.map { LocalDate.parse(it.id.removePrefix(ANNOUNCEMENT)) }
                if (months.isNotEmpty()) announced.save(announced.load() + months)
            }
        }
    }
}

private const val ANNOUNCEMENT = "validated-"
