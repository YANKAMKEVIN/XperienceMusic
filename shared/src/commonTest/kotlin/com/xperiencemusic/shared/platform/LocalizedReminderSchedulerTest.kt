package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.fakes.frenchWords
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedReminderSchedulerTest {
    private val at = LocalDateTime(2026, 10, 28, 12, 0)
    private val sent = mutableListOf<LocalNotification>()
    private val notifications = object : NotificationScheduler {
        override suspend fun replaceAll(notifications: List<LocalNotification>) {
            sent.clear()
            sent += notifications
        }
    }

    // Each text reads as its resource name and arguments: the words themselves live in strings.xml.
    private val scheduler = LocalizedReminderScheduler(
        notifications,
        strings = { resource, args -> listOf(resource.key, *args).joinToString("|") },
        loadWords = { frenchWords },
    )

    @Test
    fun everyReminderGetsItsTextsWithItsDatesSpelledOut() = runTest {
        scheduler.replaceAll(
            listOf(
                Reminder.MonthValidated(LocalDate(2026, 11, 1), listOf(LocalDate(2026, 11, 8), LocalDate(2026, 11, 13)), at),
                Reminder.RehearsalEve(LocalDate(2026, 11, 8), listOf("À toi la gloire", "Amazing Grace"), at),
                Reminder.RehearsalEve(LocalDate(2026, 11, 15), emptyList(), at),
                Reminder.ServiceEve(LocalDate(2026, 11, 13), ServiceKind.FRIDAY, at),
                Reminder.AnswersDue(LocalDate(2026, 12, 1), 3, LocalDate(2026, 11, 25), at),
            ),
        )

        assertEquals(
            listOf(
                "reminder_validated_title|novembre" to "reminder_validated_body|dim. 8 nov., ven. 13 nov.",
                "reminder_rehearsal_title" to "reminder_rehearsal_body_songs|À toi la gloire, Amazing Grace",
                "reminder_rehearsal_title" to "reminder_rehearsal_body",
                "reminder_eve_title" to "reminder_eve_friday",
                "reminder_answers_title|décembre" to "reminder_answers_body|3|25 nov.",
            ),
            sent.map { it.title to it.body },
        )
        assertEquals("validated-2026-11-01", sent.first().id)
    }

    @Test
    fun noReminderClearsTheNotificationsWithoutReadingAnyText() = runTest {
        val bare = LocalizedReminderScheduler(notifications, strings = { _, _ -> error("no text needed") }, loadWords = { error("no words needed") })
        sent += LocalNotification("old", at, "t", "b")

        bare.replaceAll(emptyList())

        assertEquals(emptyList(), sent)
    }
}
