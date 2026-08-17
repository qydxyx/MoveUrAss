package com.kegeltrainer.app.domain.catalog

import com.kegeltrainer.app.domain.model.DailyBudget
import com.kegeltrainer.app.domain.model.Goal
import com.kegeltrainer.app.domain.model.Level
import com.kegeltrainer.app.domain.model.Phase
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.Workout
import com.kegeltrainer.app.domain.model.WorkoutBlock
import com.kegeltrainer.app.domain.model.WorkoutType
import com.kegeltrainer.app.domain.model.maxDurationSec

object WorkoutCatalog {
    val all: List<Workout> = buildCatalog()

    fun byId(id: String): Workout =
        all.firstOrNull { it.id == id }
            ?: error("Unknown workout id: $id")

    fun find(id: String): Workout? = all.firstOrNull { it.id == id }

    fun matching(
        level: Level,
        type: WorkoutType,
        budget: DailyBudget,
    ): Workout {
        val budgetCap = budget.maxDurationSec()
        val same = all.filter { it.level == level && it.type == type }
        return same
            .filter { it.durationSec <= budgetCap }
            .minByOrNull { kotlin.math.abs(it.durationSec - targetDuration(budget)) }
            ?: same.minByOrNull { it.durationSec }
            ?: all.filter { it.type == type }.minByOrNull { it.durationSec }
            ?: all.first()
    }

    private fun targetDuration(budget: DailyBudget): Int = when (budget) {
        DailyBudget.SHORT -> 180
        DailyBudget.MEDIUM -> 360
        DailyBudget.LONG -> 540
    }
}

private fun buildCatalog(): List<Workout> {
    val workouts = mutableListOf<Workout>()
    Level.entries.forEach { level ->
        WorkoutType.entries.forEach { type ->
            workouts += makeWorkout(level, type, variant = 1)
            workouts += makeWorkout(level, type, variant = 2)
        }
    }
    return workouts
}

private fun makeWorkout(level: Level, type: WorkoutType, variant: Int): Workout {
    val mark = if (variant == 1) "甲" else "乙"
    val id = "${type.name.lowercase()}_${level.name.lowercase()}_$variant"
    val extra = if (variant == 2) 1 else 0
    val (title, summary, tips, goals) = copyFor(type, level, mark)
    return Workout(
        id = id,
        title = title,
        summary = summary,
        level = level,
        type = type,
        goals = goals,
        blocks = blocksFor(type, level, extra),
        coachTips = tips,
    )
}

private data class Copy(
    val title: String,
    val summary: String,
    val tips: List<String>,
    val goals: Set<Goal>,
)

private fun copyFor(type: WorkoutType, level: Level, mark: String): Copy {
    val levelName = when (level) {
        Level.BEGINNER -> "入门"
        Level.INTERMEDIATE -> "进阶"
        Level.ADVANCED -> "强化"
        Level.EXPERT -> "专家"
    }
    return when (type) {
        WorkoutType.QUICK -> Copy(
            title = "快速募集 · $levelName$mark",
            summary = "短促收缩、迅速放松，唤醒盆底的神经募集，适合作为热身或碎片化练习。",
            tips = listOf("只收会阴与肛门周围，臀部不要跟着夹。", "收缩时正常呼吸，不要憋气。"),
            goals = setOf(Goal.CONTROL, Goal.GENERAL),
        )
        WorkoutType.SLOW -> Copy(
            title = "慢速夯实 · $levelName$mark",
            summary = "缓慢收紧并保持，再完全松开。这是建立盆底肌力与膀胱控制的基础课。",
            tips = listOf("保持阶段想象把会阴轻轻上提。", "放松必须放干净，比收缩同样重要。"),
            goals = setOf(Goal.PROSTATE, Goal.BLADDER, Goal.GENERAL),
        )
        WorkoutType.ENDURANCE -> Copy(
            title = "耐力巩固 · $levelName$mark",
            summary = "更长的保持与更充分的休息，训练盆底在疲劳前的持续控制。",
            tips = listOf("如果出现抖动或代偿，提前放松，下一组再来。", "全程不要用腹肌帮忙。"),
            goals = setOf(Goal.CONTROL, Goal.PROSTATE),
        )
        WorkoutType.ELEVATOR -> Copy(
            title = "电梯控制 · $levelName$mark",
            summary = "像坐电梯一样分轻、中、最大三档上提，再逐级放下，练精细控制。",
            tips = listOf("三档都要分得清，不要一开始就用全力。", "下降时同样分段，不要一下子掉下来。"),
            goals = setOf(Goal.CONTROL, Goal.GENERAL),
        )
        WorkoutType.MIXED -> Copy(
            title = "综合循环 · $levelName$mark",
            summary = "快收、慢持交替，接近日常真正需要的控制方式，适合作为今日主课。",
            tips = listOf("快与慢切换时重新找准发力点。", "感到酸胀是正常的，刺痛则应停止。"),
            goals = setOf(Goal.PROSTATE, Goal.BLADDER, Goal.CONTROL, Goal.GENERAL),
        )
        WorkoutType.RECOVERY -> Copy(
            title = "放松恢复 · $levelName$mark",
            summary = "轻收缩配更长放松，给盆底一个恢复日，避免只练收、不练放。",
            tips = listOf("把注意力放在完全松开上。", "可以配合缓慢腹式呼吸。"),
            goals = setOf(Goal.PROSTATE, Goal.GENERAL),
        )
    }
}

private fun blocksFor(type: WorkoutType, level: Level, extra: Int): List<WorkoutBlock> {
    val hold = holdMs(level)
    val rest = restMs(level)
    val reps = repsFor(type, level) + extra
    return when (type) {
        WorkoutType.QUICK -> listOf(
            blockOf(Phase(PhaseType.CONTRACT, 1_000L), Phase(PhaseType.RELAX, 1_000L), reps),
            restBlock(rest),
            blockOf(Phase(PhaseType.CONTRACT, 1_000L), Phase(PhaseType.RELAX, 1_000L), reps),
        )
        WorkoutType.SLOW -> listOf(
            blockOf(Phase(PhaseType.CONTRACT, 1_000L), Phase(PhaseType.HOLD, hold), Phase(PhaseType.RELAX, hold), reps),
            restBlock(rest),
            blockOf(Phase(PhaseType.CONTRACT, 1_000L), Phase(PhaseType.HOLD, hold), Phase(PhaseType.RELAX, hold), (reps - 1).coerceAtLeast(4)),
        )
        WorkoutType.ENDURANCE -> {
            val longHold = hold + 4_000L
            listOf(
                blockOf(Phase(PhaseType.CONTRACT, 1_200L), Phase(PhaseType.HOLD, longHold), Phase(PhaseType.RELAX, longHold), reps),
                restBlock(rest + 4_000L),
                blockOf(Phase(PhaseType.CONTRACT, 1_200L), Phase(PhaseType.HOLD, longHold), Phase(PhaseType.RELAX, longHold), (reps - 1).coerceAtLeast(3)),
            )
        }
        WorkoutType.ELEVATOR -> listOf(
            blockOf(
                listOf(
                    Phase(PhaseType.CONTRACT, 1_600L, 1),
                    Phase(PhaseType.HOLD, 1_600L, 2),
                    Phase(PhaseType.HOLD, 1_600L, 3),
                    Phase(PhaseType.HOLD, 1_600L, 2),
                    Phase(PhaseType.RELAX, 2_400L, 1),
                ),
                reps,
            ),
            restBlock(rest),
        )
        WorkoutType.MIXED -> listOf(
            blockOf(Phase(PhaseType.CONTRACT, 1_000L), Phase(PhaseType.RELAX, 1_000L), reps + 2),
            restBlock((rest * 0.6).toLong()),
            blockOf(Phase(PhaseType.CONTRACT, 1_000L), Phase(PhaseType.HOLD, hold), Phase(PhaseType.RELAX, hold), (reps - 2).coerceAtLeast(4)),
            restBlock(rest),
            blockOf(Phase(PhaseType.CONTRACT, 1_000L), Phase(PhaseType.RELAX, 1_000L), reps),
        )
        WorkoutType.RECOVERY -> listOf(
            blockOf(Phase(PhaseType.CONTRACT, 800L), Phase(PhaseType.HOLD, hold / 2), Phase(PhaseType.RELAX, hold + 3_000L), reps),
            restBlock(rest + 2_000L),
            blockOf(Phase(PhaseType.RELAX, 8_000L), 2),
        )
    }
}

private fun blockOf(vararg phasesAndReps: Any): WorkoutBlock {
    val phases = phasesAndReps.filterIsInstance<Phase>()
    val nested = phasesAndReps.filterIsInstance<List<Phase>>().flatten()
    val reps = phasesAndReps.filterIsInstance<Int>().first()
    return WorkoutBlock(phases = phases + nested, repeat = reps)
}

private fun restBlock(ms: Long) = WorkoutBlock(listOf(Phase(PhaseType.REST, ms)), 1)

private fun holdMs(level: Level): Long = when (level) {
    Level.BEGINNER -> 5_000L
    Level.INTERMEDIATE -> 7_000L
    Level.ADVANCED -> 9_000L
    Level.EXPERT -> 10_000L
}

private fun restMs(level: Level): Long = when (level) {
    Level.BEGINNER -> 10_000L
    Level.INTERMEDIATE -> 8_000L
    Level.ADVANCED -> 7_000L
    Level.EXPERT -> 6_000L
}

private fun repsFor(type: WorkoutType, level: Level): Int {
    val base = when (level) {
        Level.BEGINNER -> 6
        Level.INTERMEDIATE -> 8
        Level.ADVANCED -> 10
        Level.EXPERT -> 12
    }
    return when (type) {
        WorkoutType.QUICK -> base + 4
        WorkoutType.SLOW -> base
        WorkoutType.ENDURANCE -> (base - 2).coerceAtLeast(4)
        WorkoutType.ELEVATOR -> (base - 3).coerceAtLeast(4)
        WorkoutType.MIXED -> base
        WorkoutType.RECOVERY -> (base - 2).coerceAtLeast(4)
    }
}
