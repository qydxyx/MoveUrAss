package com.kegeltrainer.app.domain.plan

import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.AgeRange
import com.kegeltrainer.app.domain.model.DailyBudget
import com.kegeltrainer.app.domain.model.Experience
import com.kegeltrainer.app.domain.model.Goal
import com.kegeltrainer.app.domain.model.Level
import com.kegeltrainer.app.domain.model.UserProfile
import com.kegeltrainer.app.domain.model.WorkoutType
import com.kegeltrainer.app.domain.model.maxDurationSec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanGeneratorTest {
    @Test
    fun neverTrainsStartsBeginnerEvenIfYoung() {
        val profile = profile(
            age = AgeRange.UNDER_30,
            experience = Experience.NEVER,
            goals = setOf(Goal.GENERAL),
        )
        assertEquals(Level.BEGINNER, PlanGenerator.startingLevel(profile))
    }

    @Test
    fun regularUnderThirtyStartsAdvanced() {
        val profile = profile(
            age = AgeRange.UNDER_30,
            experience = Experience.REGULAR,
            goals = setOf(Goal.CONTROL),
        )
        assertEquals(Level.ADVANCED, PlanGenerator.startingLevel(profile))
    }

    @Test
    fun overSixtyCapsAtIntermediate() {
        val profile = profile(
            age = AgeRange.OVER_60,
            experience = Experience.REGULAR,
            goals = setOf(Goal.PROSTATE),
        )
        assertEquals(Level.INTERMEDIATE, PlanGenerator.startingLevel(profile))
    }

    @Test
    fun alwaysEmitsTwentyEightDaysWithWeeklyRecovery() {
        val combos = listOf(
            profile(AgeRange.UNDER_30, Experience.NEVER, setOf(Goal.GENERAL), DailyBudget.SHORT),
            profile(AgeRange.AGE_30_45, Experience.OCCASIONAL, setOf(Goal.BLADDER), DailyBudget.MEDIUM),
            profile(AgeRange.AGE_46_60, Experience.REGULAR, setOf(Goal.CONTROL, Goal.PROSTATE), DailyBudget.LONG),
            profile(AgeRange.OVER_60, Experience.NEVER, setOf(Goal.PROSTATE, Goal.BLADDER), DailyBudget.SHORT),
        )
        combos.forEach { profile ->
            val plan = PlanGenerator.generate(profile)
            assertEquals(28, plan.size)
            assertEquals((0..27).toList(), plan.map { it.dayIndex })
            (0..3).forEach { week ->
                val weekDays = plan.filter { it.week == week + 1 }
                assertEquals(7, weekDays.size)
                val types = weekDays.map { WorkoutCatalog.byId(it.workoutId).type }
                assertTrue("week ${week + 1} should rest", types.any { it == WorkoutType.RECOVERY })
            }
        }
    }

    @Test
    fun respectsDailyBudgetWhenPossible() {
        val profile = profile(
            age = AgeRange.AGE_30_45,
            experience = Experience.OCCASIONAL,
            goals = setOf(Goal.GENERAL),
            budget = DailyBudget.SHORT,
        )
        val plan = PlanGenerator.generate(profile)
        plan.forEach { day ->
            val workout = WorkoutCatalog.byId(day.workoutId)
            assertTrue(
                "${workout.id} is ${workout.durationSec}s, over short budget",
                workout.durationSec <= DailyBudget.SHORT.maxDurationSec(),
            )
        }
    }

    @Test
    fun doesNotScheduleConsecutiveExpertEndurance() {
        val profile = profile(
            age = AgeRange.UNDER_30,
            experience = Experience.REGULAR,
            goals = setOf(Goal.CONTROL),
            budget = DailyBudget.LONG,
        )
        val plan = PlanGenerator.generate(profile)
        val types = plan.map { WorkoutCatalog.byId(it.workoutId) }
        types.zipWithNext().forEach { (a, b) ->
            val consecutive = a.type == WorkoutType.ENDURANCE &&
                b.type == WorkoutType.ENDURANCE &&
                a.level == Level.EXPERT &&
                b.level == Level.EXPERT
            assertTrue("no consecutive expert endurance", !consecutive)
        }
    }

    private fun profile(
        age: AgeRange,
        experience: Experience,
        goals: Set<Goal>,
        budget: DailyBudget = DailyBudget.MEDIUM,
        baseline: Int? = null,
    ) = UserProfile(
        ageRange = age,
        experience = experience,
        goals = goals,
        dailyBudget = budget,
        baselineHoldSec = baseline,
    )
}
