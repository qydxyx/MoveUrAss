package com.kegeltrainer.app.domain.catalog

import com.kegeltrainer.app.domain.model.PhaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCatalogTest {
    @Test
    fun hasFortyEightUniqueWorkouts() {
        assertEquals(48, WorkoutCatalog.all.size)
        assertEquals(48, WorkoutCatalog.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyWorkoutHasDurationAndRelaxation() {
        WorkoutCatalog.all.forEach { workout ->
            assertTrue(workout.id, workout.durationMs > 0)
            assertTrue(workout.id, workout.durationSec == (workout.durationMs / 1000).toInt())
            val types = workout.flattenedPhases.map { it.type }.toSet()
            assertTrue(workout.id, PhaseType.PREPARE in types)
            assertTrue(
                workout.id,
                PhaseType.CONTRACT in types || PhaseType.HOLD in types || PhaseType.RELAX in types,
            )
            assertTrue(workout.id, workout.blocks.isNotEmpty())
        }
    }
}
