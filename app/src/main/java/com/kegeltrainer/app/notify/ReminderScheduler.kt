package com.kegeltrainer.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun reschedule(minutesOfDay: Set<Int>) {
        cancelAll()
        minutesOfDay.forEach { minutes -> scheduleNext(minutes) }
    }

    fun scheduleNext(minuteOfDay: Int) {
        val triggerAt = nextTrigger(minuteOfDay)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION
            putExtra(ReminderReceiver.EXTRA_MINUTE_OF_DAY, minuteOfDay)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(minuteOfDay),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancelAll() {
        // Clear all potentially scheduled minute slots (0..1439)
        (0..1439).forEach { minute ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION
            }
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode(minute),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pending != null) {
                alarms.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun nextTrigger(minuteOfDay: Int): Long {
        val hour = (minuteOfDay / 60).coerceIn(0, 23)
        val minute = (minuteOfDay % 60).coerceIn(0, 59)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun requestCode(minuteOfDay: Int) = 7000 + minuteOfDay
}
