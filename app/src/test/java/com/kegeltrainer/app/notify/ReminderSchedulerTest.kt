package com.kegeltrainer.app.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderSchedulerTest {
    private fun millisAt(hour: Int, minute: Int, second: Int = 0, dayOffset: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 10 + dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun returnsNullWhenEmptyOrInvalid() {
        val now = millisAt(10, 0)
        assertNull(ReminderScheduler.computeNextTrigger(emptySet(), now))
        assertNull(ReminderScheduler.computeNextTrigger(setOf(-1, 1440), now))
    }

    @Test
    fun picksEarliestUpcomingReminder() {
        val now = millisAt(10, 30)
        val next = ReminderScheduler.computeNextTrigger(
            minutesOfDay = setOf(9 * 60, 12 * 60 + 15, 21 * 60),
            nowMs = now,
        )
        assertEquals(12 * 60 + 15, next?.first)
        assertEquals(millisAt(12, 15), next?.second)
    }

    @Test
    fun wrapsToNextDayWhenAllPassed() {
        val now = millisAt(22, 0)
        val next = ReminderScheduler.computeNextTrigger(
            minutesOfDay = setOf(9 * 60, 21 * 60),
            nowMs = now,
        )
        assertEquals(9 * 60, next?.first)
        assertEquals(millisAt(9, 0, dayOffset = 1), next?.second)
    }

    @Test
    fun skipsJustFiredMinuteEvenIfFiredSlightlyEarly() {
        val now = millisAt(20, 59, second = 50)
        val next = ReminderScheduler.computeNextTrigger(
            minutesOfDay = setOf(21 * 60),
            nowMs = now,
            justFiredMinute = 21 * 60,
        )
        assertEquals(21 * 60, next?.first)
        assertTrue((next?.second ?: 0L) > now + 60_000L)
        assertEquals(millisAt(21, 0, dayOffset = 1), next?.second)
    }
}
