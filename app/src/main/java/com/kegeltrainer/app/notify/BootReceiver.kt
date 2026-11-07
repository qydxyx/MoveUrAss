package com.kegeltrainer.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kegeltrainer.app.data.prefs.UserPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var prefs: UserPrefs

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = prefs.settings.first()
                scheduler.reschedule(settings.reminderMinutes)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
