package com.kegeltrainer.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
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
    val reminderHours: Set<Int> = setOf(21),
)

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

    suspend fun setReminderHours(hours: Set<Int>) = edit {
        it[REMINDERS] = hours.sorted().joinToString(",")
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit(block)
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        onboarded = this[ONBOARDED] ?: false,
        voiceEnabled = this[VOICE] ?: true,
        hapticEnabled = this[HAPTIC] ?: true,
        stealthEnabled = this[STEALTH] ?: false,
        reminderHours = this[REMINDERS]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: setOf(21),
    )

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val VOICE = booleanPreferencesKey("voice")
        val HAPTIC = booleanPreferencesKey("haptic")
        val STEALTH = booleanPreferencesKey("stealth")
        val REMINDERS = stringPreferencesKey("reminders")
    }
}
