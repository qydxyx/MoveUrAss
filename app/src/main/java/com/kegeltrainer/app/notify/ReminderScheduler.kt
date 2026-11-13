package com.kegeltrainer.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun reschedule(minutesOfDay: Set<Int>, justFiredMinute: Int? = null) {
        cancelScheduled()
        val next = computeNextTrigger(minutesOfDay, System.currentTimeMillis(), justFiredMinute) ?: return
        scheduleExact(next.first, next.second)
    }

    private fun scheduleExact(minuteOfDay: Int, triggerAt: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION
            putExtra(ReminderReceiver.EXTRA_MINUTE_OF_DAY, minuteOfDay)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            SINGLE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            alarms?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancelAll() {
        cancelScheduled()
    }

    private fun cancelScheduled() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION
        }
        val pending = PendingIntent.getBroadcast(
            context,
            SINGLE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarms?.cancel(pending)
            pending.cancel()
        }
    }

    internal companion object {
        private const val SINGLE_ALARM_REQUEST_CODE = 7000

        fun computeNextTrigger(
            minutesOfDay: Set<Int>,
            nowMs: Long,
            justFiredMinute: Int? = null,
        ): Pair<Int, Long>? {
            val valid = minutesOfDay.filter { it in 0..1439 }
            if (valid.isEmpty()) return null
            return valid
                .map { minute -> minute to nextTrigger(minute, nowMs, justFiredMinute) }
                .minByOrNull { it.second }
        }

        private fun nextTrigger(minuteOfDay: Int, nowMs: Long, justFiredMinute: Int?): Long {
            val hour = (minuteOfDay / 60).coerceIn(0, 23)
            val minute = (minuteOfDay % 60).coerceIn(0, 59)
            val target = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                val alreadyPassed = timeInMillis <= nowMs
                val isSameJustFiredWindow = justFiredMinute == minuteOfDay && abs(timeInMillis - nowMs) < 60_000L
                if (alreadyPassed || isSameJustFiredWindow) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return target.timeInMillis
        }
    }
}
