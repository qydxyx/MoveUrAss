package com.kegeltrainer.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kegeltrainer.app.MainActivity
import com.kegeltrainer.app.R
import com.kegeltrainer.app.data.prefs.UserPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var prefs: UserPrefs

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val minuteOfDay = when {
            intent.hasExtra(EXTRA_MINUTE_OF_DAY) -> intent.getIntExtra(EXTRA_MINUTE_OF_DAY, -1)
            intent.hasExtra(EXTRA_HOUR) -> intent.getIntExtra(EXTRA_HOUR, -1) * 60
            else -> -1
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminderMinutes = prefs.settings.first().reminderMinutes
                val shouldNotify = reminderMinutes.isNotEmpty() &&
                    (minuteOfDay < 0 || minuteOfDay in reminderMinutes)
                if (shouldNotify) {
                    showNotification(context)
                }
                scheduler.reschedule(
                    minutesOfDay = reminderMinutes,
                    justFiredMinute = minuteOfDay.takeIf { it in 0..1439 },
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "训练提醒", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val launch = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("该做今日盆底训练了")
            .setContentIntent(launch)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFY_ID, notification)
    }

    companion object {
        const val ACTION = "com.kegeltrainer.app.REMIND"
        const val EXTRA_MINUTE_OF_DAY = "minute_of_day"
        const val EXTRA_HOUR = "hour" // backward compatibility
        private const val CHANNEL_ID = "kegel_reminders"
        private const val NOTIFY_ID = 42
    }
}
