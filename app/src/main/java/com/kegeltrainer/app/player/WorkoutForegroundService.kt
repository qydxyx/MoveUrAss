package com.kegeltrainer.app.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kegeltrainer.app.MainActivity
import com.kegeltrainer.app.R
import com.kegeltrainer.app.domain.model.displayLabel
import com.kegeltrainer.app.domain.player.WorkoutSessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WorkoutForegroundService : Service() {

    @Inject
    lateinit var sessionManager: WorkoutSessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var observerJob: Job? = null
    private var stopDelayJob: Job? = null
    private var completionHandled = false
    private var lastNotificationKey: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        // Immediately start foreground to satisfy Android 8+ & 14+ 5-second requirement
        val initialNotification = buildNotification(
            title = getString(R.string.app_name),
            content = "训练准备中…",
            isPaused = false,
        )
        startServiceForeground(initialNotification)
        observeSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        acquireWakeLock()
        when (intent?.action) {
            ACTION_PAUSE_RESUME -> sessionManager.pauseOrResume()
            ACTION_STOP -> {
                stopDelayJob?.cancel()
                stopDelayJob = null
                scope.launch {
                    sessionManager.saveSession(completed = false)
                    sessionManager.stopSession()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopDelayJob?.cancel()
        stopDelayJob = null
        observerJob?.cancel()
        scope.cancel()
        releaseWakeLock()
    }

    private fun startServiceForeground(notification: Notification) {
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun updateNotification(notification: Notification) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun observeSession() {
        observerJob = scope.launch {
            sessionManager.state.collectLatest { state ->
                val snap = state.snapshot
                val workout = state.workout

                if (state.isFinished) {
                    if (!completionHandled) {
                        completionHandled = true
                        if (workout != null) {
                            showCompletionNotification(workout.title, state.settings.stealthEnabled)
                        }
                        // Keep wakeLock alive briefly for 3s so the completion chime and TTS voice finish playing in screen-off
                        stopDelayJob?.cancel()
                        stopDelayJob = scope.launch {
                            delay(3000)
                            stopSelf()
                        }
                    }
                    return@collectLatest
                }

                if (snap == null || workout == null) {
                    // Session starting up or idle, do not stopSelf!
                    return@collectLatest
                }

                completionHandled = false
                stopDelayJob?.cancel()
                stopDelayJob = null

                val title = getString(R.string.app_name)
                val content = if (state.settings.stealthEnabled) {
                    "训练进行中 · 剩余 ${snap.remainingSec}s"
                } else {
                    "${workout.title} · ${snap.phase.type.displayLabel()} ${snap.remainingSec}s"
                }

                // Debounce notifications: only update when visible content actually changes (approx once per sec)
                // Avoids 20 IPC calls per sec flooding Binder & stalling MainLooper
                val notificationKey = "${snap.phase.type}_${snap.remainingSec}_${snap.isPaused}_${state.settings.stealthEnabled}"
                if (notificationKey == lastNotificationKey) {
                    return@collectLatest
                }
                lastNotificationKey = notificationKey

                val notification = buildNotification(
                    title = title,
                    content = content,
                    isPaused = snap.isPaused,
                )
                updateNotification(notification)
            }
        }
    }

    private fun buildNotification(title: String, content: String, isPaused: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pauseResumeIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_PAUSE_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, WorkoutForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "继续" else "暂停",
                pauseResumeIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "结束",
                stopIntent,
            )

        return builder.build()
    }

    private fun showCompletionNotification(workoutTitle: String, isStealth: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (isStealth) getString(R.string.app_name) else "🎉 训练已完成！"
        val text = if (isStealth) "今日训练已顺利完成，已记录到履历。" else "$workoutTitle · 今日训练已记录"

        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        if (wakeLock?.isHeld == true) return
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MoveUrAss::WorkoutWakeLock",
        ).apply {
            setReferenceCounted(false)
            acquire(45 * 60 * 1000L) // 45 minutes safety timeout
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val playbackChannel = NotificationChannel(
                CHANNEL_ID,
                "训练跟练",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "锁屏与后台训练节奏提醒"
                setShowBadge(false)
            }

            val completionChannel = NotificationChannel(
                COMPLETION_CHANNEL_ID,
                "训练完成通知",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "训练完成提醒与记录"
                setShowBadge(true)
            }

            manager.createNotificationChannel(playbackChannel)
            manager.createNotificationChannel(completionChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "kegel_workout_playback"
        const val COMPLETION_CHANNEL_ID = "kegel_workout_completion"
        const val NOTIFICATION_ID = 43
        const val COMPLETION_NOTIFICATION_ID = 44
        const val ACTION_PAUSE_RESUME = "com.kegeltrainer.app.PAUSE_RESUME"
        const val ACTION_STOP = "com.kegeltrainer.app.STOP_WORKOUT"

        fun start(context: Context) {
            val intent = Intent(context, WorkoutForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WorkoutForegroundService::class.java))
        }
    }
}
