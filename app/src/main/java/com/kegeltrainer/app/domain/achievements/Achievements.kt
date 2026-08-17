package com.kegeltrainer.app.domain.achievements

import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.Level
import com.kegeltrainer.app.domain.model.SessionRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AchievementDef(
    val id: String,
    val title: String,
    val detail: String,
)

object Achievements {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first", "迈出第一步", "完成第一节训练"),
        AchievementDef("streak_3", "三日之约", "连续训练 3 天"),
        AchievementDef("streak_7", "一周节律", "连续训练 7 天"),
        AchievementDef("streak_21", "习惯成形", "连续训练 21 天"),
        AchievementDef("count_10", "十次呼吸", "累计完成 10 次训练"),
        AchievementDef("count_30", "月积跬步", "累计完成 30 次训练"),
        AchievementDef("count_60", "盆底常客", "累计完成 60 次训练"),
        AchievementDef("advanced", "进入强化", "首次完成强化或专家课"),
    )

    fun unlockedIds(
        sessions: List<SessionRecord>,
        today: LocalDate = LocalDate.now(),
    ): Set<String> {
        val done = sessions.filter { it.completed }
        if (done.isEmpty()) return emptySet()
        val streak = currentStreak(done, today)
        val count = done.size
        val hasAdvanced = done.any { session ->
            val level = WorkoutCatalog.find(session.workoutId)?.level
            level == Level.ADVANCED || level == Level.EXPERT
        }
        return buildSet {
            add("first")
            if (streak >= 3) add("streak_3")
            if (streak >= 7) add("streak_7")
            if (streak >= 21) add("streak_21")
            if (count >= 10) add("count_10")
            if (count >= 30) add("count_30")
            if (count >= 60) add("count_60")
            if (hasAdvanced) add("advanced")
        }
    }

    fun currentStreak(sessions: List<SessionRecord>, today: LocalDate = LocalDate.now()): Int {
        val days = sessions.filter { it.completed }
            .map { Instant.ofEpochMilli(it.endedAt).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSet()
        if (days.isEmpty()) return 0
        var cursor = if (today in days) today else today.minusDays(1)
        if (cursor !in days) return 0
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun longestStreak(sessions: List<SessionRecord>): Int {
        val days = sessions.filter { it.completed }
            .map { Instant.ofEpochMilli(it.endedAt).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSortedSet()
        if (days.isEmpty()) return 0
        var best = 1
        var current = 1
        val list = days.toList()
        for (i in 1 until list.size) {
            if (list[i] == list[i - 1].plusDays(1)) {
                current++
                best = maxOf(best, current)
            } else if (list[i] != list[i - 1]) {
                current = 1
            }
        }
        return best
    }
}
