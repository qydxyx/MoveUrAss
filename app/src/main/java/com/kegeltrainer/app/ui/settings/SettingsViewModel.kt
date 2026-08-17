package com.kegeltrainer.app.ui.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kegeltrainer.app.data.prefs.AppSettings
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.notify.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: TrainerRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    val onboarded: StateFlow<Boolean?> = repo.settings
        .map<AppSettings, Boolean?> { it.onboarded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setVoice(value: Boolean) = viewModelScope.launch { repo.setVoice(value) }
    fun setHaptic(value: Boolean) = viewModelScope.launch { repo.setHaptic(value) }
    fun setStealth(value: Boolean) = viewModelScope.launch { repo.setStealth(value) }

    fun toggleReminder(hour: Int, activity: Activity) {
        viewModelScope.launch {
            ensureNotificationPermission(activity)
            val current = settings.value.reminderHours.toMutableSet()
            if (!current.add(hour)) current.remove(hour)
            repo.setReminderHours(current)
            scheduler.reschedule(current)
        }
    }

    private fun ensureNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001,
            )
        }
    }
}
