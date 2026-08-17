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

    fun reschedule(hours: Set<Int>) {
        cancelAll()
        hours.forEach { hour -> scheduleNext(hour) }
    }

    fun scheduleNext(hour: Int) {
        val triggerAt = nextTrigger(hour)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(hour),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancelAll() {
        (0..23).forEach { hour ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION
            }
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode(hour),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pending != null) {
                alarms.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun nextTrigger(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun requestCode(hour: Int) = 7100 + hour
}
