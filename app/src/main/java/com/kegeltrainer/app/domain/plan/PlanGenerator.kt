package com.kegeltrainer.app.domain.plan

import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.AgeRange
import com.kegeltrainer.app.domain.model.DailyBudget
import com.kegeltrainer.app.domain.model.Experience
import com.kegeltrainer.app.domain.model.Goal
import com.kegeltrainer.app.domain.model.Level
import com.kegeltrainer.app.domain.model.PlannedDay
import com.kegeltrainer.app.domain.model.UserProfile
import com.kegeltrainer.app.domain.model.Workout
import com.kegeltrainer.app.domain.model.WorkoutType
import com.kegeltrainer.app.domain.model.maxDurationSec

object PlanGenerator {
    const val PLAN_DAYS = 28

    fun generate(
        profile: UserProfile,
        catalog: List<Workout> = WorkoutCatalog.all,
    ): List<PlannedDay> {
        require(catalog.isNotEmpty()) { "Catalog must not be empty" }
        val startLevel = startingLevel(profile)
        return (0 until PLAN_DAYS).map { day ->
            val week = day / 7
            val weekday = day % 7
            val level = levelForWeek(startLevel, week)
            val type = typeFor(profile, weekday, level)
            val workout = pick(catalog, level, type, profile.dailyBudget)
            PlannedDay(dayIndex = day, workoutId = workout.id, week = week + 1)
        }
    }

    fun startingLevel(profile: UserProfile): Level {
        var level = when (profile.experience) {
            Experience.NEVER -> Level.BEGINNER
            Experience.OCCASIONAL -> Level.INTERMEDIATE
            Experience.REGULAR -> Level.ADVANCED
        }
        if (profile.ageRange == AgeRange.OVER_60 && level.ordinal > Level.INTERMEDIATE.ordinal) {
            level = Level.INTERMEDIATE
        }
        if (profile.ageRange == AgeRange.UNDER_30 && profile.experience == Experience.REGULAR) {
            level = Level.ADVANCED
        }
        val baseline = profile.baselineHoldSec
        if (baseline != null) {
            level = when {
                baseline < 5 -> minLevel(level, Level.BEGINNER)
                baseline < 8 -> minLevel(level, Level.INTERMEDIATE)
                baseline >= 12 && profile.experience != Experience.NEVER ->
                    maxLevel(level, Level.ADVANCED)
                else -> level
            }
        }
        if (profile.experience == Experience.NEVER) {
            level = Level.BEGINNER
        }
        return level
    }

    private fun levelForWeek(start: Level, week: Int): Level {
        val bumped = start.ordinal + week.coerceAtMost(2)
        return Level.entries[bumped.coerceAtMost(Level.EXPERT.ordinal)]
    }

    private fun typeFor(profile: UserProfile, weekday: Int, level: Level): WorkoutType {
        if (weekday == 6) return WorkoutType.RECOVERY
        val prostate = Goal.PROSTATE in profile.goals
        val bladder = Goal.BLADDER in profile.goals
        val control = Goal.CONTROL in profile.goals
        val weekPattern = when {
            bladder && !control -> listOf(
                WorkoutType.SLOW,
                WorkoutType.SLOW,
                WorkoutType.MIXED,
                WorkoutType.SLOW,
                WorkoutType.QUICK,
                WorkoutType.MIXED,
            )
            control && !bladder -> listOf(
                WorkoutType.MIXED,
                WorkoutType.ELEVATOR,
                WorkoutType.ENDURANCE,
                WorkoutType.QUICK,
                WorkoutType.MIXED,
                WorkoutType.ENDURANCE,
            )
            prostate -> listOf(
                WorkoutType.MIXED,
                WorkoutType.SLOW,
                WorkoutType.MIXED,
                WorkoutType.SLOW,
                WorkoutType.ELEVATOR,
                WorkoutType.MIXED,
            )
            else -> listOf(
                WorkoutType.MIXED,
                WorkoutType.QUICK,
                WorkoutType.SLOW,
                WorkoutType.MIXED,
                WorkoutType.ELEVATOR,
                WorkoutType.SLOW,
            )
        }
        var type = weekPattern[weekday]
        if (type == WorkoutType.ENDURANCE && level == Level.EXPERT && weekday == 5) {
            type = WorkoutType.MIXED
        }
        return type
    }

    private fun pick(
        catalog: List<Workout>,
        level: Level,
        type: WorkoutType,
        budget: DailyBudget,
    ): Workout {
        val cap = budget.maxDurationSec()
        val candidates = catalog.filter { it.type == type && it.level.ordinal <= level.ordinal }
        val underCap = candidates.filter { it.durationSec <= cap }
        return underCap
            .sortedWith(
                compareByDescending<Workout> { it.level.ordinal }
                    .thenBy { kotlin.math.abs(it.durationSec - target(budget)) },
            )
            .firstOrNull()
            ?: candidates.minByOrNull { it.durationSec }
            ?: catalog.filter { it.type == type }.minByOrNull { it.durationSec }
            ?: catalog.first()
    }

    private fun target(budget: DailyBudget): Int = when (budget) {
        DailyBudget.SHORT -> 180
        DailyBudget.MEDIUM -> 360
        DailyBudget.LONG -> 540
    }

    private fun minLevel(a: Level, b: Level): Level =
        if (a.ordinal < b.ordinal) a else b

    private fun maxLevel(a: Level, b: Level): Level =
        if (a.ordinal > b.ordinal) a else b
}
