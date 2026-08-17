package com.kegeltrainer.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val ageRange: String,
    val experience: String,
    val goalsCsv: String,
    val dailyBudget: String,
    val baselineHoldSec: Int?,
    val planStartedEpochDay: Long,
    val onboardedAt: Long,
)

@Entity(tableName = "plan_days")
data class PlanDayEntity(
    @PrimaryKey val dayIndex: Int,
    val workoutId: String,
    val week: Int,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val completed: Boolean,
    val isPlanSession: Boolean,
    val contractionCount: Int,
    val epochDay: Long,
)
