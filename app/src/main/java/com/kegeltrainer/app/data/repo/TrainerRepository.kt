package com.kegeltrainer.app.data.repo

import com.kegeltrainer.app.data.db.PlanDayEntity
import com.kegeltrainer.app.data.db.PlanDao
import com.kegeltrainer.app.data.db.ProfileDao
import com.kegeltrainer.app.data.db.ProfileEntity
import com.kegeltrainer.app.data.db.SessionDao
import com.kegeltrainer.app.data.db.SessionEntity
import com.kegeltrainer.app.data.prefs.UserPrefs
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.AgeRange
import com.kegeltrainer.app.domain.model.DailyBudget
import com.kegeltrainer.app.domain.model.Experience
import com.kegeltrainer.app.domain.model.Goal
import com.kegeltrainer.app.domain.model.PlannedDay
import com.kegeltrainer.app.domain.model.SessionRecord
import com.kegeltrainer.app.domain.model.UserProfile
import com.kegeltrainer.app.domain.plan.PlanGenerator
import com.kegeltrainer.app.ui.theme.ThemeMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class TrainerRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val planDao: PlanDao,
    private val sessionDao: SessionDao,
    private val prefs: UserPrefs,
) {
    val settings = prefs.settings

    val profile: Flow<UserProfile?> = profileDao.observe().map { it?.toDomain() }

    val planDays: Flow<List<PlannedDay>> = planDao.observeAll().map { rows ->
        rows.map { PlannedDay(it.dayIndex, it.workoutId, it.week) }
    }

    val sessions: Flow<List<SessionRecord>> = sessionDao.observeAll().map { rows ->
        rows.map { it.toDomain() }
    }

    data class TodayState(
        val profile: UserProfile?,
        val dayIndex: Int,
        val planned: PlannedDay?,
        val todaySessions: List<SessionRecord>,
        val allSessions: List<SessionRecord>,
        val planCompletedToday: Boolean,
    )

    val todayState: Flow<TodayState> = combine(
        profile,
        planDays,
        sessions,
    ) { profile, plan, sessions ->
        val start = profile?.planStartedEpochDay
        val today = LocalDate.now().toEpochDay()
        val dayIndex = if (start == null) 0 else {
            val raw = (today - start).toInt()
            val cycle = PlanGenerator.PLAN_DAYS
            ((raw % cycle) + cycle) % cycle
        }
        val planned = plan.firstOrNull { it.dayIndex == dayIndex }
        val todaySessions = sessions.filter {
            Instant.ofEpochMilli(it.endedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                .toEpochDay() == today
        }
        TodayState(
            profile = profile,
            dayIndex = dayIndex,
            planned = planned,
            todaySessions = todaySessions,
            allSessions = sessions,
            planCompletedToday = todaySessions.any { it.completed && it.isPlanSession },
        )
    }

    suspend fun completeOnboarding(profile: UserProfile) {
        val startDay = LocalDate.now().toEpochDay()
        val stored = profile.copy(planStartedEpochDay = startDay)
        profileDao.upsert(stored.toEntity(onboardedAt = System.currentTimeMillis()))
        val plan = PlanGenerator.generate(stored)
        planDao.clear()
        planDao.upsertAll(plan.map { PlanDayEntity(it.dayIndex, it.workoutId, it.week) })
        prefs.setOnboarded(true)
    }

    suspend fun recordSession(record: SessionRecord) {
        val epochDay = Instant.ofEpochMilli(record.endedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
        sessionDao.insert(
            SessionEntity(
                workoutId = record.workoutId,
                startedAt = record.startedAt,
                endedAt = record.endedAt,
                durationMs = record.durationMs,
                completed = record.completed,
                isPlanSession = record.isPlanSession,
                contractionCount = record.contractionCount,
                epochDay = epochDay,
            ),
        )
    }

    suspend fun setVoice(enabled: Boolean) = prefs.setVoice(enabled)
    suspend fun setHaptic(enabled: Boolean) = prefs.setHaptic(enabled)
    suspend fun setStealth(enabled: Boolean) = prefs.setStealth(enabled)
    suspend fun setReminderHours(hours: Set<Int>) = prefs.setReminderHours(hours)
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    fun workoutTitle(id: String): String = WorkoutCatalog.find(id)?.title ?: id
}

private fun ProfileEntity.toDomain(): UserProfile = UserProfile(
    ageRange = AgeRange.valueOf(ageRange),
    experience = Experience.valueOf(experience),
    goals = if (goalsCsv.isBlank()) emptySet() else goalsCsv.split(",").map { Goal.valueOf(it) }.toSet(),
    dailyBudget = DailyBudget.valueOf(dailyBudget),
    baselineHoldSec = baselineHoldSec,
    planStartedEpochDay = planStartedEpochDay,
)

private fun UserProfile.toEntity(onboardedAt: Long): ProfileEntity = ProfileEntity(
    ageRange = ageRange.name,
    experience = experience.name,
    goalsCsv = goals.joinToString(",") { it.name },
    dailyBudget = dailyBudget.name,
    baselineHoldSec = baselineHoldSec,
    planStartedEpochDay = planStartedEpochDay,
    onboardedAt = onboardedAt,
)

private fun SessionEntity.toDomain(): SessionRecord = SessionRecord(
    id = id,
    workoutId = workoutId,
    startedAt = startedAt,
    endedAt = endedAt,
    durationMs = durationMs,
    completed = completed,
    isPlanSession = isPlanSession,
    contractionCount = contractionCount,
)
