package com.kegeltrainer.app.domain.player

import com.kegeltrainer.app.domain.model.Phase
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.Workout

class WorkoutEngine(
    val phases: List<Phase>,
    private val nowMs: () -> Long,
) {
    constructor(workout: Workout, nowMs: () -> Long) : this(workout.flattenedPhases, nowMs)

    val totalMs: Long = phases.sumOf { it.durationMs }

    private var startedAt: Long = nowMs()
    private var accumulatedMs: Long = 0L
    private var pauseStartedAt: Long? = null

    val contractionTotal: Int = countContractionCycles(phases)

    fun snapshot(): Snapshot {
        val elapsed = elapsedMs().coerceAtMost(totalMs)
        if (phases.isEmpty() || elapsed >= totalMs) {
            return Snapshot(
                phaseIndex = (phases.lastIndex).coerceAtLeast(0),
                phase = phases.lastOrNull() ?: Phase(PhaseType.REST, 0L),
                remainingMs = 0L,
                elapsedMs = totalMs,
                totalMs = totalMs,
                contractionIndex = contractionTotal,
                contractionTotal = contractionTotal,
                isPaused = pauseStartedAt != null,
                isFinished = true,
            )
        }
        var acc = 0L
        var cycleIndex = 0
        var inEffort = false
        phases.forEachIndexed { index, phase ->
            val isEffort = phase.isContraction
            if (isEffort && !inEffort) {
                cycleIndex++
            }
            inEffort = isEffort
            val end = acc + phase.durationMs
            if (elapsed < end) {
                return Snapshot(
                    phaseIndex = index,
                    phase = phase,
                    remainingMs = end - elapsed,
                    elapsedMs = elapsed,
                    totalMs = totalMs,
                    contractionIndex = cycleIndex,
                    contractionTotal = contractionTotal,
                    isPaused = pauseStartedAt != null,
                    isFinished = false,
                )
            }
            acc = end
        }
        return Snapshot(
            phaseIndex = (phases.lastIndex).coerceAtLeast(0),
            phase = phases.lastOrNull() ?: Phase(PhaseType.REST, 0L),
            remainingMs = 0L,
            elapsedMs = totalMs,
            totalMs = totalMs,
            contractionIndex = contractionTotal,
            contractionTotal = contractionTotal,
            isPaused = pauseStartedAt != null,
            isFinished = true,
        )
    }

    fun pause() {
        if (pauseStartedAt != null || elapsedMs() >= totalMs) return
        accumulatedMs += nowMs() - startedAt
        pauseStartedAt = nowMs()
    }

    fun resume() {
        if (pauseStartedAt == null) return
        startedAt = nowMs()
        pauseStartedAt = null
    }

    private fun elapsedMs(): Long {
        return if (pauseStartedAt != null) {
            accumulatedMs.coerceAtLeast(0L)
        } else {
            (accumulatedMs + nowMs() - startedAt).coerceAtLeast(0L)
        }
    }

    data class Snapshot(
        val phaseIndex: Int,
        val phase: Phase,
        val remainingMs: Long,
        val elapsedMs: Long,
        val totalMs: Long,
        val contractionIndex: Int,
        val contractionTotal: Int,
        val isPaused: Boolean,
        val isFinished: Boolean,
    ) {
        val progress: Float
            get() = if (totalMs == 0L) 1f else (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        val remainingSec: Int get() = ((remainingMs + 999L) / 1000L).toInt().coerceAtLeast(0)
    }
}

private val Phase.isContraction: Boolean
    get() = type == PhaseType.CONTRACT || type == PhaseType.HOLD

private fun countContractionCycles(phases: List<Phase>): Int {
    var count = 0
    var inEffort = false
    phases.forEach { phase ->
        val isEffort = phase.isContraction
        if (isEffort && !inEffort) {
            count++
        }
        inEffort = isEffort
    }
    return count
}
