package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.model.ServiceKind
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import com.xperiencemusic.shared.presentation.common.FormattingWords
import com.xperiencemusic.shared.presentation.common.loadFormattingWords
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.reminder_answers_body
import com.xperiencemusic.shared.resources.reminder_answers_title
import com.xperiencemusic.shared.resources.reminder_eve_friday
import com.xperiencemusic.shared.resources.reminder_eve_sunday
import com.xperiencemusic.shared.resources.reminder_eve_title
import com.xperiencemusic.shared.resources.reminder_rehearsal_body
import com.xperiencemusic.shared.resources.reminder_rehearsal_body_songs
import com.xperiencemusic.shared.resources.reminder_rehearsal_title
import com.xperiencemusic.shared.resources.reminder_validated_body
import com.xperiencemusic.shared.resources.reminder_validated_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** Reads a string resource with its arguments; the resources themselves outside of tests. */
fun interface ReminderStrings {
    suspend fun get(resource: StringResource, vararg args: Any): String
}

/**
 * The edge between the domain's reminders and the phone: words come from the string resources
 * here, then [notifications] schedules them.
 */
class LocalizedReminderScheduler(
    private val notifications: NotificationScheduler,
    private val strings: ReminderStrings = ReminderStrings { resource, args -> getString(resource, *args) },
    private val loadWords: suspend () -> FormattingWords = ::loadFormattingWords,
) : ReminderScheduler {
    override suspend fun replaceAll(reminders: List<Reminder>) {
        if (reminders.isEmpty()) {
            notifications.replaceAll(emptyList())
            return
        }
        val words = loadWords()
        notifications.replaceAll(reminders.map { it.toNotification(words) })
    }

    private suspend fun Reminder.toNotification(words: FormattingWords): LocalNotification {
        val (title, body) = when (this) {
            is Reminder.MonthValidated -> strings.get(Res.string.reminder_validated_title, words.month(month)) to
                strings.get(Res.string.reminder_validated_body, services.joinToString(", ") { words.shortDayAndDate(it) })
            is Reminder.RehearsalEve -> strings.get(Res.string.reminder_rehearsal_title) to
                if (songTitles.isEmpty()) {
                    strings.get(Res.string.reminder_rehearsal_body)
                } else {
                    strings.get(Res.string.reminder_rehearsal_body_songs, songTitles.joinToString(", "))
                }
            is Reminder.ServiceEve -> strings.get(Res.string.reminder_eve_title) to
                strings.get(if (kind == ServiceKind.FRIDAY) Res.string.reminder_eve_friday else Res.string.reminder_eve_sunday)
            is Reminder.AnswersDue -> strings.get(Res.string.reminder_answers_title, words.month(month)) to
                strings.get(Res.string.reminder_answers_body, missing, words.dayAndMonth(answerBy))
        }
        return LocalNotification(id, at, title, body)
    }
}
