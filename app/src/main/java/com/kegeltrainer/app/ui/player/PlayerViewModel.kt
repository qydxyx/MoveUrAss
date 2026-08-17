package com.kegeltrainer.app.ui.player

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kegeltrainer.app.data.prefs.AppSettings
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.knowledge.KnowledgeArticles
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.SessionRecord
import com.kegeltrainer.app.domain.model.Workout
import com.kegeltrainer.app.domain.player.WorkoutEngine
import com.kegeltrainer.app.haptic.HapticCoach
import com.kegeltrainer.app.voice.TtsCoach
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUi(
    val workout: Workout,
    val snapshot: WorkoutEngine.Snapshot,
    val voice: Boolean,
    val haptic: Boolean,
    val stealth: Boolean,
    val confirmExit: Boolean = false,
    val finished: Boolean = false,
    val tip: String = "",
    val recorded: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TrainerRepository,
    private val tts: TtsCoach,
    private val haptic: HapticCoach,
) : ViewModel() {
    private val workoutId: String = checkNotNull(savedStateHandle["workoutId"])
    private val isPlan: Boolean = savedStateHandle["plan"] ?: false
    val workout: Workout = WorkoutCatalog.byId(workoutId)

    private val engine = WorkoutEngine(workout) { SystemClock.elapsedRealtime() }
    private val startedAt = System.currentTimeMillis()
    private var lastPhaseIndex = -1
    private var lastSpokenSec = -1
    private var ticker: Job? = null
    private var settings = AppSettings()

    private val _ui = MutableStateFlow(
        PlayerUi(
            workout = workout,
            snapshot = engine.snapshot(),
            voice = true,
            haptic = true,
            stealth = false,
        ),
    )
    val ui: StateFlow<PlayerUi> = _ui

    init {
        viewModelScope.launch {
            repo.settings.collect { value ->
                settings = value
                _ui.update {
                    it.copy(
                        voice = value.voiceEnabled,
                        haptic = value.hapticEnabled,
                        stealth = value.stealthEnabled,
                    )
                }
            }
        }
        startTicker()
        announce(engine.snapshot())
    }

    fun toggleVoice() {
        viewModelScope.launch { repo.setVoice(!_ui.value.voice) }
    }

    fun toggleHaptic() {
        viewModelScope.launch { repo.setHaptic(!_ui.value.haptic) }
    }

    fun toggleStealth() {
        viewModelScope.launch { repo.setStealth(!_ui.value.stealth) }
    }

    fun pauseOrResume() {
        if (_ui.value.finished) return
        if (engine.snapshot().isPaused) engine.resume() else engine.pause()
        publish()
    }

    fun requestExit() {
        if (_ui.value.finished) return
        if (!engine.snapshot().isPaused) engine.pause()
        _ui.update { it.copy(confirmExit = true) }
        publish()
    }

    fun dismissExit() = _ui.update { it.copy(confirmExit = false) }

    fun confirmExit(onDone: () -> Unit) {
        viewModelScope.launch {
            save(completed = false)
            onDone()
        }
    }

    fun leaveFinished(onDone: () -> Unit) = onDone()

    fun onBackground() {
        if (!_ui.value.finished && !engine.snapshot().isPaused) {
            engine.pause()
            publish()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val snap = engine.snapshot()
                if (snap.phaseIndex != lastPhaseIndex) {
                    announce(snap)
                    lastPhaseIndex = snap.phaseIndex
                    lastSpokenSec = -1
                } else if (!snap.isPaused && !snap.isFinished && snap.remainingSec in 1..3 && snap.remainingSec != lastSpokenSec) {
                    if (_ui.value.voice) tts.speakCountdown(snap.remainingSec)
                    lastSpokenSec = snap.remainingSec
                }
                if (snap.isFinished && !_ui.value.finished) {
                    finish()
                    break
                }
                publish()
                delay(50)
            }
        }
    }

    private fun announce(snap: WorkoutEngine.Snapshot) {
        if (snap.isFinished) return
        if (_ui.value.voice) tts.speakPhase(snap.phase.type)
        if (_ui.value.haptic && snap.phase.type != PhaseType.REST) haptic.onPhase(snap.phase.type)
    }

    private fun finish() {
        ticker?.cancel()
        val tip = workout.coachTips.randomOrNull()
            ?: KnowledgeArticles.rotatingTips.random()
        _ui.update {
            it.copy(
                finished = true,
                snapshot = engine.snapshot(),
                tip = tip,
            )
        }
        viewModelScope.launch { save(completed = true) }
    }

    private suspend fun save(completed: Boolean) {
        if (_ui.value.recorded) return
        val snap = engine.snapshot()
        repo.recordSession(
            SessionRecord(
                workoutId = workout.id,
                startedAt = startedAt,
                endedAt = System.currentTimeMillis(),
                durationMs = snap.elapsedMs,
                completed = completed,
                isPlanSession = isPlan,
                contractionCount = if (completed) workout.contractionCount else snap.contractionIndex,
            ),
        )
        _ui.update { it.copy(recorded = true) }
    }

    private fun publish() {
        _ui.update { it.copy(snapshot = engine.snapshot()) }
    }
}
