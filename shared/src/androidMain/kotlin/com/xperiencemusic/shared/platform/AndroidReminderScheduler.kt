package com.xperiencemusic.shared.platform

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xperiencemusic.shared.domain.model.Reminder
import com.xperiencemusic.shared.domain.repository.ReminderScheduler
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock

/**
 * Alarms that wake [ReminderReceiver] at each reminder's time; reminders already due are shown at
 * once. The ids scheduled last time are kept so they can be cancelled when the plan changes.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    override suspend fun replaceAll(reminders: List<Reminder>) {
        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().forEach { id -> alarms.cancel(pendingIntent(id, null)) }
        val now = Clock.System.now()
        val scheduled = mutableSetOf<String>()
        reminders.forEach { reminder ->
            val at = reminder.at.toInstant(TimeZone.currentSystemDefault())
            if (at <= now) {
                showReminder(context, reminder.id, reminder.title, reminder.body)
            } else {
                // Inexact on purpose: a few minutes late is fine and needs no special permission.
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), pendingIntent(reminder.id, reminder))
                scheduled += reminder.id
            }
        }
        prefs.edit().putStringSet(KEY_IDS, scheduled).apply()
    }

    private fun pendingIntent(id: String, reminder: Reminder?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction("xm.reminder.$id")
        if (reminder != null) {
            intent.putExtra(EXTRA_ID, reminder.id).putExtra(EXTRA_TITLE, reminder.title).putExtra(EXTRA_BODY, reminder.body)
        }
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    internal companion object {
        const val KEY_IDS = "scheduled"
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showReminder(
            context,
            intent.getStringExtra(AndroidReminderScheduler.EXTRA_ID) ?: return,
            intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE).orEmpty(),
            intent.getStringExtra(AndroidReminderScheduler.EXTRA_BODY).orEmpty(),
        )
    }
}

private const val CHANNEL_ID = "services"

internal fun showReminder(context: Context, id: String, title: String, body: String) {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Services et répétitions", NotificationManager.IMPORTANCE_DEFAULT),
    )
    val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
        PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
    }
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    try {
        manager.notify(id.hashCode(), notification)
    } catch (e: SecurityException) {
        // Permission withdrawn between the check and the post.
    }
}
