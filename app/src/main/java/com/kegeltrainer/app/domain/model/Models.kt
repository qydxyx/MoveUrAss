package com.kegeltrainer.app.domain.model

enum class PhaseType { PREPARE, CONTRACT, HOLD, RELAX, REST }

enum class Level { BEGINNER, INTERMEDIATE, ADVANCED, EXPERT }

enum class WorkoutType { QUICK, SLOW, ENDURANCE, ELEVATOR, MIXED, RECOVERY }

enum class Goal { PROSTATE, BLADDER, CONTROL, GENERAL }

enum class AgeRange { UNDER_30, AGE_30_45, AGE_46_60, OVER_60 }

enum class Experience { NEVER, OCCASIONAL, REGULAR }

enum class DailyBudget { SHORT, MEDIUM, LONG }

data class Phase(
    val type: PhaseType,
    val durationMs: Long,
    val intensity: Int = 1,
)

data class WorkoutBlock(
    val phases: List<Phase>,
    val repeat: Int,
)

data class Workout(
    val id: String,
    val title: String,
    val summary: String,
    val level: Level,
    val type: WorkoutType,
    val goals: Set<Goal>,
    val blocks: List<WorkoutBlock>,
    val coachTips: List<String>,
) {
    val durationMs: Long
        get() = blocks.sumOf { block ->
            block.phases.sumOf { it.durationMs } * block.repeat
        }

    val durationSec: Int get() = (durationMs / 1000L).toInt()

    val flattenedPhases: List<Phase>
        get() = buildList {
            add(Phase(PhaseType.PREPARE, 3_000L))
            blocks.forEach { block ->
                repeat(block.repeat) { addAll(block.phases) }
            }
        }

    val contractionCount: Int
        get() = flattenedPhases.count {
            it.type == PhaseType.CONTRACT || it.type == PhaseType.HOLD
        }
}

data class UserProfile(
    val ageRange: AgeRange,
    val experience: Experience,
    val goals: Set<Goal>,
    val dailyBudget: DailyBudget,
    val baselineHoldSec: Int? = null,
    val planStartedEpochDay: Long = 0L,
)

data class PlannedDay(
    val dayIndex: Int,
    val workoutId: String,
    val week: Int,
)

data class SessionRecord(
    val id: Long = 0,
    val workoutId: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val completed: Boolean,
    val isPlanSession: Boolean,
    val contractionCount: Int,
)

data class KnowledgeArticle(
    val id: String,
    val title: String,
    val subtitle: String,
    val body: String,
)

fun Level.displayName(): String = when (this) {
    Level.BEGINNER -> "入门"
    Level.INTERMEDIATE -> "进阶"
    Level.ADVANCED -> "强化"
    Level.EXPERT -> "专家"
}

fun WorkoutType.displayName(): String = when (this) {
    WorkoutType.QUICK -> "快速收缩"
    WorkoutType.SLOW -> "慢速保持"
    WorkoutType.ENDURANCE -> "耐力"
    WorkoutType.ELEVATOR -> "电梯控制"
    WorkoutType.MIXED -> "综合循环"
    WorkoutType.RECOVERY -> "放松恢复"
}

fun Goal.displayName(): String = when (this) {
    Goal.PROSTATE -> "前列腺舒适"
    Goal.BLADDER -> "膀胱控制"
    Goal.CONTROL -> "收缩控制力"
    Goal.GENERAL -> "综合盆底力量"
}

fun PhaseType.spokenCue(): String = when (this) {
    PhaseType.PREPARE -> "准备"
    PhaseType.CONTRACT -> "收缩"
    PhaseType.HOLD -> "保持"
    PhaseType.RELAX -> "放松"
    PhaseType.REST -> "休息"
}

fun PhaseType.displayLabel(): String = spokenCue()

fun AgeRange.displayName(): String = when (this) {
    AgeRange.UNDER_30 -> "30 岁以下"
    AgeRange.AGE_30_45 -> "30–45 岁"
    AgeRange.AGE_46_60 -> "46–60 岁"
    AgeRange.OVER_60 -> "60 岁以上"
}

fun Experience.displayName(): String = when (this) {
    Experience.NEVER -> "从未练过"
    Experience.OCCASIONAL -> "偶尔练过"
    Experience.REGULAR -> "规律训练"
}

fun DailyBudget.displayName(): String = when (this) {
    DailyBudget.SHORT -> "约 3 分钟"
    DailyBudget.MEDIUM -> "约 6 分钟"
    DailyBudget.LONG -> "约 10 分钟"
}

fun DailyBudget.maxDurationSec(): Int = when (this) {
    DailyBudget.SHORT -> 210
    DailyBudget.MEDIUM -> 420
    DailyBudget.LONG -> 700
}

fun formatDuration(totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return if (minutes == 0) "${seconds}秒" else if (seconds == 0) "${minutes}分钟" else "${minutes}分${seconds}秒"
}
