package com.kegeltrainer.app.data.backup

import androidx.room.withTransaction
import com.kegeltrainer.app.data.db.AppDatabase
import com.kegeltrainer.app.data.db.PlanDayEntity
import com.kegeltrainer.app.data.db.ProfileEntity
import com.kegeltrainer.app.data.db.SessionEntity
import com.kegeltrainer.app.data.prefs.AppSettings
import com.kegeltrainer.app.data.prefs.UserPrefs
import com.kegeltrainer.app.domain.model.AgeRange
import com.kegeltrainer.app.domain.model.DailyBudget
import com.kegeltrainer.app.domain.model.Experience
import com.kegeltrainer.app.domain.model.Goal
import com.kegeltrainer.app.domain.model.PlannedDay
import com.kegeltrainer.app.domain.model.SessionRecord
import com.kegeltrainer.app.domain.model.UserProfile
import com.kegeltrainer.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val profile: BackupProfile? = null,
    val planDays: List<BackupPlanDay> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
)

@Serializable
data class BackupProfile(
    val ageRange: String,
    val experience: String,
    val goals: List<String>,
    val dailyBudget: String,
    val baselineHoldSec: Int? = null,
    val planStartedEpochDay: Long = 0L,
    val onboardedAt: Long = 0L,
)

@Serializable
data class BackupPlanDay(
    val dayIndex: Int,
    val workoutId: String,
    val week: Int,
)

@Serializable
data class BackupSession(
    val workoutId: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val completed: Boolean,
    val isPlanSession: Boolean,
    val contractionCount: Int,
    val epochDay: Long,
)

@Serializable
data class BackupSettings(
    val voiceEnabled: Boolean = true,
    val completionSoundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val stealthEnabled: Boolean = false,
    val backgroundPlayEnabled: Boolean = true,
    val reminderMinutes: List<Int> = listOf(21 * 60),
    val themeMode: String = "SYSTEM",
)

@Singleton
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    private val prefs: UserPrefs,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    suspend fun exportJson(): String {
        val profile = db.profileDao().get()?.toBackup()
        val planDays = db.planDao().getAll().map { it.toBackup() }
        val sessions = db.sessionDao().getAll().map { it.toBackup() }
        val settings = prefs.settings.first().toBackup()

        val payload = BackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            profile = profile,
            planDays = planDays,
            sessions = sessions,
            settings = settings,
        )
        return json.encodeToString(payload)
    }

    suspend fun importJson(jsonContent: String): Result<Unit> = runCatching {
        val payload = json.decodeFromString<BackupPayload>(jsonContent)

        db.withTransaction {
            db.profileDao().clear()
            payload.profile?.let {
                db.profileDao().upsert(it.toEntity())
            }

            db.planDao().clear()
            if (payload.planDays.isNotEmpty()) {
                db.planDao().upsertAll(payload.planDays.map { it.toEntity() })
            }

            db.sessionDao().clear()
            if (payload.sessions.isNotEmpty()) {
                db.sessionDao().insertAll(payload.sessions.map { it.toEntity() })
            }
        }

        // Restore user preferences
        prefs.setOnboarded(payload.profile != null)
        prefs.setVoice(payload.settings.voiceEnabled)
        prefs.setCompletionSound(payload.settings.completionSoundEnabled)
        prefs.setHaptic(payload.settings.hapticEnabled)
        prefs.setStealth(payload.settings.stealthEnabled)
        prefs.setBackgroundPlay(payload.settings.backgroundPlayEnabled)
        prefs.setReminderMinutes(payload.settings.reminderMinutes.toSet())
        val themeMode = runCatching { ThemeMode.valueOf(payload.settings.themeMode) }.getOrDefault(ThemeMode.SYSTEM)
        prefs.setThemeMode(themeMode)
    }
}

private fun ProfileEntity.toBackup() = BackupProfile(
    ageRange = ageRange,
    experience = experience,
    goals = if (goalsCsv.isBlank()) emptyList() else goalsCsv.split(","),
    dailyBudget = dailyBudget,
    baselineHoldSec = baselineHoldSec,
    planStartedEpochDay = planStartedEpochDay,
    onboardedAt = onboardedAt,
)

private fun BackupProfile.toEntity() = ProfileEntity(
    id = 1,
    ageRange = ageRange,
    experience = experience,
    goalsCsv = goals.joinToString(","),
    dailyBudget = dailyBudget,
    baselineHoldSec = baselineHoldSec,
    planStartedEpochDay = planStartedEpochDay,
    onboardedAt = onboardedAt,
)

private fun PlanDayEntity.toBackup() = BackupPlanDay(
    dayIndex = dayIndex,
    workoutId = workoutId,
    week = week,
)

private fun BackupPlanDay.toEntity() = PlanDayEntity(
    dayIndex = dayIndex,
    workoutId = workoutId,
    week = week,
)

private fun SessionEntity.toBackup() = BackupSession(
    workoutId = workoutId,
    startedAt = startedAt,
    endedAt = endedAt,
    durationMs = durationMs,
    completed = completed,
    isPlanSession = isPlanSession,
    contractionCount = contractionCount,
    epochDay = epochDay,
)

private fun BackupSession.toEntity() = SessionEntity(
    id = 0,
    workoutId = workoutId,
    startedAt = startedAt,
    endedAt = endedAt,
    durationMs = durationMs,
    completed = completed,
    isPlanSession = isPlanSession,
    contractionCount = contractionCount,
    epochDay = epochDay,
)

private fun AppSettings.toBackup() = BackupSettings(
    voiceEnabled = voiceEnabled,
    completionSoundEnabled = completionSoundEnabled,
    hapticEnabled = hapticEnabled,
    stealthEnabled = stealthEnabled,
    backgroundPlayEnabled = backgroundPlayEnabled,
    reminderMinutes = reminderMinutes.sorted(),
    themeMode = themeMode.name,
)
