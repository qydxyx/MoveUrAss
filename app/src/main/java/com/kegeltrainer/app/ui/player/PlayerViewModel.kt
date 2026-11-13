package com.kegeltrainer.app.ui.player

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.Workout
import com.kegeltrainer.app.domain.player.WorkoutEngine
import com.kegeltrainer.app.domain.player.WorkoutSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    val backgroundPlay: Boolean = true,
    val settingsLoaded: Boolean = false,
    val confirmExit: Boolean = false,
    val finished: Boolean = false,
    val exitedExternally: Boolean = false,
    val tip: String = "",
    val recorded: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TrainerRepository,
    private val sessionManager: WorkoutSessionManager,
) : ViewModel() {
    private val workoutId: String = checkNotNull(savedStateHandle["workoutId"])
    private val isPlan: Boolean = savedStateHandle["plan"] ?: false
    val workout: Workout = WorkoutCatalog.byId(workoutId)

    private var sessionObservedActive = false
    private var exitingLocally = false

    private val _ui = MutableStateFlow(
        PlayerUi(
            workout = workout,
            snapshot = WorkoutEngine(workout) { SystemClock.elapsedRealtime() }.snapshot(),
            voice = true,
            haptic = true,
            stealth = false,
            backgroundPlay = true,
        ),
    )
    val ui: StateFlow<PlayerUi> = _ui

    init {
        sessionManager.startSession(workout, isPlan)
        viewModelScope.launch {
            sessionManager.state.collect { state ->
                val snap = state.snapshot
                val activeWorkout = state.workout
                if (activeWorkout?.id == workoutId && snap != null) {
                    sessionObservedActive = true
                    _ui.update {
                        it.copy(
                            snapshot = snap,
                            voice = state.settings.voiceEnabled,
                            haptic = state.settings.hapticEnabled,
                            stealth = state.settings.stealthEnabled,
                            backgroundPlay = state.settings.backgroundPlayEnabled,
                            settingsLoaded = state.settingsLoaded,
                            finished = state.isFinished,
                            tip = state.tip,
                            recorded = state.recorded,
                        )
                    }
                } else if (sessionObservedActive && !exitingLocally && activeWorkout == null && !state.isFinished) {
                    _ui.update { it.copy(exitedExternally = true) }
                } else if (!sessionObservedActive) {
                    _ui.update {
                        it.copy(
                            voice = state.settings.voiceEnabled,
                            haptic = state.settings.hapticEnabled,
                            stealth = state.settings.stealthEnabled,
                            backgroundPlay = state.settings.backgroundPlayEnabled,
                            settingsLoaded = state.settingsLoaded,
                        )
                    }
                }
            }
        }
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
        sessionManager.pauseOrResume()
    }

    fun requestExit() {
        if (_ui.value.finished) return
        sessionManager.pause()
        _ui.update { it.copy(confirmExit = true) }
    }

    fun dismissExit() {
        _ui.update { it.copy(confirmExit = false) }
        sessionManager.resume()
    }

    fun confirmExit(onDone: () -> Unit) {
        exitingLocally = true
        viewModelScope.launch {
            sessionManager.saveSession(completed = false)
            sessionManager.stopSession()
            onDone()
        }
    }

    fun leaveFinished(onDone: () -> Unit) {
        exitingLocally = true
        sessionManager.stopSession()
        onDone()
    }

    fun onBackground() {
        if (!_ui.value.backgroundPlay && !_ui.value.finished) {
            sessionManager.pause()
        }
    }
}
