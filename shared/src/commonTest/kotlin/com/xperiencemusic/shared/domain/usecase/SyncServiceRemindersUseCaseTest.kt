package com.xperiencemusic.shared.domain.usecase

import com.xperiencemusic.shared.data.repository.InMemoryMembersRepository
import com.xperiencemusic.shared.data.repository.InMemoryPlanningRepository
import com.xperiencemusic.shared.domain.model.PlanningMonth
import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.model.ServiceSignup
import com.xperiencemusic.shared.fakes.FakeAnnouncedMonthsStore
import com.xperiencemusic.shared.fakes.FakeReminderScheduler
import com.xperiencemusic.shared.fakes.FakeSessionRepository
import com.xperiencemusic.shared.fakes.member
import com.xperiencemusic.shared.fakes.schedule
import com.xperiencemusic.shared.fakes.setlistRepository
import com.xperiencemusic.shared.fakes.songRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncServiceRemindersUseCaseTest {
    private val november = LocalDate(2026, 11, 1)
    private val sunday8 = LocalDate(2026, 11, 8)
    private val scheduler = FakeReminderScheduler()
    private val announced = FakeAnnouncedMonthsStore()

    private fun sync(): SyncServiceRemindersUseCase {
        val sessions = FakeSessionRepository(member())
        val planning = InMemoryPlanningRepository(
            listOf(PlanningMonth(november, null, isValidated = true)),
            listOf(ServiceSignup(sunday8, "me", true)),
        )
        return SyncServiceRemindersUseCase(
            ObservePlanningUseCase(planning, InMemoryMembersRepository(sessions), ObserveCurrentMemberUseCase(sessions)),
            schedule(setlistRepository(), songRepository()),
            scheduler,
            announced,
        ) { LocalDateTime(2026, 10, 28, 12, 0) }
    }

    @Test
    fun `les rappels sont programmés et le mois validé n'est annoncé qu'une fois`() = runTest {
        val first = assertIs<ReminderSync.Scheduled>(sync()().first())
        assertEquals(setOf(november), first.announcedMonths)
        assertEquals(setOf(november), announced.months)
        assertEquals(first.reminders, scheduler.scheduled)
        assertTrue(first.reminders.any { it is Reminder.MonthValidated })

        val second = assertIs<ReminderSync.Scheduled>(sync()().first())
        assertTrue(second.announcedMonths.isEmpty())
        assertTrue(second.reminders.none { it is Reminder.MonthValidated })
    }

    @Test
    fun `sans notifications rien n'est retenu comme annoncé`() = runTest {
        scheduler.isUnavailable = true
        assertEquals(ReminderSync.Unavailable, sync()().first())
        assertTrue(announced.months.isEmpty())
        assertNull(scheduler.scheduled)
    }
}
