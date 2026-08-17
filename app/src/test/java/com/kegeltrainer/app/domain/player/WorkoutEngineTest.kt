package com.kegeltrainer.app.domain.player

import com.kegeltrainer.app.domain.model.Phase
import com.kegeltrainer.app.domain.model.PhaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutEngineTest {
    private class Clock(var t: Long = 0L) {
        fun now(): Long = t
    }

    @Test
    fun walksPhasesOnElapsedRealtime() {
        val clock = Clock()
        val engine = WorkoutEngine(
            listOf(
                Phase(PhaseType.CONTRACT, 1_000L),
                Phase(PhaseType.RELAX, 2_000L),
            ),
            clock::now,
        )
        assertEquals(PhaseType.CONTRACT, engine.snapshot().phase.type)
        clock.t = 999
        assertEquals(PhaseType.CONTRACT, engine.snapshot().phase.type)
        assertEquals(1, engine.snapshot().remainingSec)
        clock.t = 1_000
        assertEquals(PhaseType.RELAX, engine.snapshot().phase.type)
        clock.t = 2_999
        assertFalse(engine.snapshot().isFinished)
        clock.t = 3_000
        assertTrue(engine.snapshot().isFinished)
        assertEquals(3_000L, engine.snapshot().elapsedMs)
    }

    @Test
    fun pauseFreezesElapsedTime() {
        val clock = Clock()
        val engine = WorkoutEngine(listOf(Phase(PhaseType.HOLD, 5_000L)), clock::now)
        clock.t = 1_200
        engine.pause()
        clock.t = 8_000
        val paused = engine.snapshot()
        assertTrue(paused.isPaused)
        assertEquals(1_200L, paused.elapsedMs)
        assertEquals(PhaseType.HOLD, paused.phase.type)
        engine.resume()
        clock.t = 8_500
        assertEquals(1_700L, engine.snapshot().elapsedMs)
        assertFalse(engine.snapshot().isPaused)
    }

    @Test
    fun contractionIndexCountsHoldAndContract() {
        val clock = Clock()
        val engine = WorkoutEngine(
            listOf(
                Phase(PhaseType.PREPARE, 500L),
                Phase(PhaseType.CONTRACT, 500L),
                Phase(PhaseType.HOLD, 500L),
                Phase(PhaseType.RELAX, 500L),
            ),
            clock::now,
        )
        assertEquals(0, engine.snapshot().contractionIndex)
        clock.t = 500
        assertEquals(1, engine.snapshot().contractionIndex)
        clock.t = 1_000
        assertEquals(2, engine.snapshot().contractionIndex)
        clock.t = 1_500
        assertEquals(2, engine.snapshot().contractionIndex)
        assertEquals(2, engine.contractionTotal)
    }

    @Test
    fun emptyPhasesFinishImmediately() {
        val engine = WorkoutEngine(emptyList()) { 0L }
        assertTrue(engine.snapshot().isFinished)
    }
}
