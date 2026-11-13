package com.kegeltrainer.app.domain.player

import android.os.SystemClock
import com.kegeltrainer.app.data.prefs.AppSettings
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.domain.knowledge.KnowledgeArticles
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.SessionRecord
import com.kegeltrainer.app.domain.model.Workout
import com.kegeltrainer.app.haptic.HapticCoach
import com.kegeltrainer.app.sound.SoundCoach
import com.kegeltrainer.app.voice.TtsCoach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class SessionState(
    val workout: Workout? = null,
    val isPlan: Boolean = false,
    val snapshot: WorkoutEngine.Snapshot? = null,
    val settings: AppSettings = AppSettings(),
    val settingsLoaded: Boolean = false,
    val isFinished: Boolean = false,
    val recorded: Boolean = false,
    val tip: String = "",
)

@Singleton
class WorkoutSessionManager @Inject constructor(
    private val repo: TrainerRepository,
    private val tts: TtsCoach,
    private val haptic: HapticCoach,
    private val soundCoach: SoundCoach,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val isPausedFlow = MutableStateFlow(false)

    @Volatile
    private var engine: WorkoutEngine? = null
    private var startJob: Job? = null
    private var ticker: Job? = null
    private var finishVoiceJob: Job? = null
    private var startedAtMs: Long = 0L
    private var lastPhaseIndex: Int = -1
    private var lastSpokenSec: Int = -1

    init {
        scope.launch {
            repo.settings.collect { newSettings ->
                _state.update { it.copy(settings = newSettings, settingsLoaded = true) }
            }
        }
    }

    fun startSession(workout: Workout, isPlan: Boolean) {
        val current = _state.value
        if (current.workout?.id == workout.id && !current.isFinished && (engine != null || startJob?.isActive == true)) {
            return
        }
        startJob?.cancel()
        ticker?.cancel()
        ticker = null
        finishVoiceJob?.cancel()
        finishVoiceJob = null
        tts.stop()
        engine = null
        isPausedFlow.value = false

        startJob = scope.launch {
            val initialSettings = if (_state.value.settingsLoaded) {
                _state.value.settings
            } else {
                repo.settings.first()
            }
            val newEngine = WorkoutEngine(workout) { SystemClock.elapsedRealtime() }
            engine = newEngine
            startedAtMs = System.currentTimeMillis()
            lastPhaseIndex = -1
            lastSpokenSec = -1
            isPausedFlow.value = false

            _state.value = SessionState(
                workout = workout,
                isPlan = isPlan,
                snapshot = newEngine.snapshot(),
                settings = initialSettings,
                settingsLoaded = true,
                isFinished = false,
                recorded = false,
                tip = "",
            )
            startTicker()
        }
    }

    fun pauseOrResume() {
        val eng = engine ?: return
        val current = eng.snapshot()
        if (current.isFinished) return
        if (current.isPaused) {
            eng.resume()
            isPausedFlow.value = false
        } else {
            eng.pause()
            isPausedFlow.value = true
            tts.stop()
        }
        publish()
    }

    fun pause() {
        val eng = engine ?: return
        if (!eng.snapshot().isPaused && !eng.snapshot().isFinished) {
            eng.pause()
            isPausedFlow.value = true
            tts.stop()
            publish()
        }
    }

    fun resume() {
        val eng = engine ?: return
        if (eng.snapshot().isPaused && !eng.snapshot().isFinished) {
            eng.resume()
            isPausedFlow.value = false
            publish()
        }
    }

    fun stopSession() {
        startJob?.cancel()
        startJob = null
        ticker?.cancel()
        ticker = null
        finishVoiceJob?.cancel()
        finishVoiceJob = null
        isPausedFlow.value = false
        tts.stop()
        engine = null
        val current = _state.value
        _state.value = SessionState(
            settings = current.settings,
            settingsLoaded = current.settingsLoaded,
        )
    }

    suspend fun saveSession(completed: Boolean) {
        val currentState = _state.value
        if (currentState.recorded || currentState.workout == null || engine == null) return
        val snap = engine?.snapshot() ?: return
        val workout = currentState.workout
        repo.recordSession(
            SessionRecord(
                workoutId = workout.id,
                startedAt = startedAtMs,
                endedAt = System.currentTimeMillis(),
                durationMs = snap.elapsedMs,
                completed = completed,
                isPlanSession = currentState.isPlan,
                contractionCount = if (completed) workout.contractionCount else snap.contractionIndex,
            ),
        )
        _state.update { it.copy(recorded = true) }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val eng = engine ?: break
                val snap = eng.snapshot()
                if (snap.isPaused && !snap.isFinished) {
                    publish()
                    isPausedFlow.first { !it }
                    continue
                }
                if (snap.phaseIndex != lastPhaseIndex) {
                    announce(snap)
                    lastPhaseIndex = snap.phaseIndex
                    lastSpokenSec = -1
                } else if (shouldCountDown(snap) && snap.remainingSec != lastSpokenSec) {
                    if (_state.value.settings.voiceEnabled && !_state.value.settings.stealthEnabled) {
                        tts.speakCountdown(snap.remainingSec)
                    }
                    lastSpokenSec = snap.remainingSec
                }

                if (snap.isFinished && !_state.value.isFinished) {
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
        val settings = _state.value.settings
        if (settings.voiceEnabled && !settings.stealthEnabled) {
            tts.speakPhase(snap.phase.type, snap.phase.durationMs)
        }
        if (settings.hapticEnabled && snap.phase.type != PhaseType.REST) {
            haptic.onPhase(snap.phase.type)
        }
    }

    private fun shouldCountDown(snap: WorkoutEngine.Snapshot): Boolean {
        if (snap.isPaused || snap.isFinished) return false
        if (snap.remainingSec !in 1..3) return false
        return snap.phase.durationMs > 3_000L
    }

    private fun finish() {
        ticker?.cancel()
        isPausedFlow.value = false
        val workout = _state.value.workout ?: return
        val tip = workout.coachTips.randomOrNull()
            ?: KnowledgeArticles.rotatingTips.random()
        _state.update {
            it.copy(
                isFinished = true,
                snapshot = engine?.snapshot(),
                tip = tip,
            )
        }
        val settings = _state.value.settings
        if (!settings.stealthEnabled) {
            if (settings.completionSoundEnabled) {
                soundCoach.playCompletionSound()
            }
            if (settings.voiceEnabled) {
                val voiceDelayMs = if (settings.completionSoundEnabled) 700L else 100L
                finishVoiceJob?.cancel()
                finishVoiceJob = scope.launch {
                    delay(voiceDelayMs)
                    tts.speak("训练完成")
                }
            }
        }
        if (settings.hapticEnabled) {
            haptic.onComplete()
        }
        scope.launch {
            saveSession(completed = true)
        }
    }

    private fun publish() {
        val snap = engine?.snapshot() ?: return
        _state.update { it.copy(snapshot = snap) }
    }
}
