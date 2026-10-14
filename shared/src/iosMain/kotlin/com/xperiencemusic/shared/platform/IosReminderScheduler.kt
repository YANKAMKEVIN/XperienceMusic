package com.xperiencemusic.shared.platform

import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Local notifications. The app posts no others, so every pending one is ours to replace. */
class IosReminderScheduler : ReminderScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    // Shown even while the app is open: a validated planning is announced right away.
    private val foreground = object : NSObject(), UNUserNotificationCenterDelegateProtocol {
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
        ) {
            withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound)
        }
    }

    init {
        center.delegate = foreground
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun replaceAll(reminders: List<Reminder>) {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, _ -> }
        center.removeAllPendingNotificationRequests()
        val now = Clock.System.now()
        reminders.forEach { reminder ->
            val content = UNMutableNotificationContent().apply {
                setTitle(reminder.title)
                setBody(reminder.body)
                setSound(UNNotificationSound.defaultSound)
            }
            val isDue = reminder.at.toInstant(TimeZone.currentSystemDefault()) <= now
            val trigger = if (isDue) {
                null
            } else {
                val at = NSDateComponents().apply {
                    setYear(reminder.at.year.toLong())
                    setMonth(reminder.at.month.ordinal.toLong() + 1)
                    setDay(reminder.at.day.toLong())
                    setHour(reminder.at.hour.toLong())
                    setMinute(reminder.at.minute.toLong())
                }
                UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(at, repeats = false)
            }
            center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(reminder.id, content, trigger), null)
        }
    }
}
