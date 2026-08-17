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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var prefs: UserPrefs

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val hours = runBlocking { prefs.settings.first().reminderHours }
            scheduler.reschedule(hours)
            return
        }
        if (intent.action != ACTION) return
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        showNotification(context)
        if (hour in 0..23) scheduler.scheduleNext(hour)
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
            .setContentTitle("腺动")
            .setContentText("该做今日盆底训练了")
            .setContentIntent(launch)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFY_ID, notification)
    }

    companion object {
        const val ACTION = "com.kegeltrainer.app.REMIND"
        const val EXTRA_HOUR = "hour"
        private const val CHANNEL_ID = "kegel_reminders"
        private const val NOTIFY_ID = 42
    }
}
