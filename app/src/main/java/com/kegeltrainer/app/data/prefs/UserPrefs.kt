package com.kegeltrainer.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kegeltrainer.app.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val onboarded: Boolean = false,
    val voiceEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val stealthEnabled: Boolean = false,
    val backgroundPlayEnabled: Boolean = true,
    val completionSoundEnabled: Boolean = true,
    val reminderMinutes: Set<Int> = setOf(21 * 60), // 21:00 = 1260 minutes
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    // For legacy/quick compatibility
    val reminderHours: Set<Int>
        get() = reminderMinutes.map { it / 60 }.toSet()
}

@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("kegel_settings")
    }

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun setOnboarded(value: Boolean) = edit { it[ONBOARDED] = value }

    suspend fun setVoice(value: Boolean) = edit { it[VOICE] = value }

    suspend fun setHaptic(value: Boolean) = edit { it[HAPTIC] = value }

    suspend fun setStealth(value: Boolean) = edit { it[STEALTH] = value }

    suspend fun setBackgroundPlay(value: Boolean) = edit { it[BACKGROUND_PLAY] = value }

    suspend fun setCompletionSound(value: Boolean) = edit { it[COMPLETION_SOUND] = value }

    suspend fun setReminderMinutes(minutes: Set<Int>) = edit {
        it[REMINDERS_MINUTES] = minutes.sorted().joinToString(",")
    }

    suspend fun setReminderHours(hours: Set<Int>) = setReminderMinutes(hours.map { it * 60 }.toSet())

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[THEME] = mode.name }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit(block)
    }

    private fun Preferences.toSettings(): AppSettings {
        val minutesString = this[REMINDERS_MINUTES]
        val reminders: Set<Int> = if (!minutesString.isNullOrBlank()) {
            minutesString.split(",")
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        } else {
            // Backward compatibility with legacy REMINDERS ("8,12,21" -> hour values)
            this[REMINDERS]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.map { it * 60 }
                ?.toSet()
                ?: setOf(21 * 60)
        }

        return AppSettings(
            onboarded = this[ONBOARDED] ?: false,
            voiceEnabled = this[VOICE] ?: true,
            hapticEnabled = this[HAPTIC] ?: true,
            stealthEnabled = this[STEALTH] ?: false,
            backgroundPlayEnabled = this[BACKGROUND_PLAY] ?: true,
            completionSoundEnabled = this[COMPLETION_SOUND] ?: true,
            reminderMinutes = reminders,
            themeMode = this[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
        )
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val VOICE = booleanPreferencesKey("voice")
        val HAPTIC = booleanPreferencesKey("haptic")
        val STEALTH = booleanPreferencesKey("stealth")
        val BACKGROUND_PLAY = booleanPreferencesKey("background_play")
        val COMPLETION_SOUND = booleanPreferencesKey("completion_sound")
        val REMINDERS = stringPreferencesKey("reminders")
        val REMINDERS_MINUTES = stringPreferencesKey("reminders_minutes")
        val THEME = stringPreferencesKey("theme_mode")
    }
}
