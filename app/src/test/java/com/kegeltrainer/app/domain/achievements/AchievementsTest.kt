package com.kegeltrainer.app.domain.achievements

import com.kegeltrainer.app.domain.model.SessionRecord
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {
    @Test
    fun streakCountsBackFromToday() {
        val today = LocalDate.of(2026, 8, 17)
        val sessions = listOf(
            session(today),
            session(today.minusDays(1)),
            session(today.minusDays(2)),
        )
        assertEquals(3, Achievements.currentStreak(sessions, today))
    }

    @Test
    fun streakAllowsMissingToday() {
        val today = LocalDate.of(2026, 8, 17)
        val sessions = listOf(session(today.minusDays(1)), session(today.minusDays(2)))
        assertEquals(2, Achievements.currentStreak(sessions, today))
    }

    @Test
    fun unlocksFirstAndCountMilestones() {
        val today = LocalDate.of(2026, 8, 17)
        val sessions = (0 until 10).map { session(today.minusDays(it.toLong())) }
        val ids = Achievements.unlockedIds(sessions, today)
        assertTrue(ids.contains("first"))
        assertTrue(ids.contains("count_10"))
        assertTrue(ids.contains("streak_7"))
    }

    private fun session(day: LocalDate) = SessionRecord(
        workoutId = "quick_beginner_1",
        startedAt = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endedAt = day.atTime(21, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        durationMs = 180_000,
        completed = true,
        isPlanSession = true,
        contractionCount = 20,
    )
}
