package com.kegeltrainer.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.AgeRange
import com.kegeltrainer.app.domain.model.DailyBudget
import com.kegeltrainer.app.domain.model.Experience
import com.kegeltrainer.app.domain.model.Goal
import com.kegeltrainer.app.domain.model.PlannedDay
import com.kegeltrainer.app.domain.model.UserProfile
import com.kegeltrainer.app.domain.plan.PlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardStep {
    WELCOME, DISCLAIMER, AGE, EXPERIENCE, GOALS, BUDGET, GUIDE, BASELINE, PREVIEW
}

data class OnboardUi(
    val step: OnboardStep = OnboardStep.WELCOME,
    val agreed: Boolean = false,
    val ageRange: AgeRange? = null,
    val experience: Experience? = null,
    val goals: Set<Goal> = emptySet(),
    val budget: DailyBudget? = null,
    val guidePage: Int = 0,
    val baselineHoldSec: Int? = null,
    val baselineRunning: Boolean = false,
    val baselineElapsedSec: Int = 0,
    val preview: List<PlannedDay> = emptyList(),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: TrainerRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(OnboardUi())
    val ui: StateFlow<OnboardUi> = _ui

    fun agree(value: Boolean) = _ui.update { it.copy(agreed = value) }

    fun chooseAge(value: AgeRange) = _ui.update { it.copy(ageRange = value) }

    fun chooseExperience(value: Experience) = _ui.update { it.copy(experience = value) }

    fun toggleGoal(goal: Goal) = _ui.update {
        val next = it.goals.toMutableSet()
        if (!next.add(goal)) next.remove(goal)
        it.copy(goals = next)
    }

    fun chooseBudget(value: DailyBudget) = _ui.update { it.copy(budget = value) }

    fun setGuidePage(page: Int) = _ui.update { it.copy(guidePage = page) }

    fun skipBaseline() {
        _ui.update { it.copy(baselineHoldSec = null, baselineRunning = false) }
        goNext()
    }

    fun startBaseline() = _ui.update { it.copy(baselineRunning = true, baselineElapsedSec = 0) }

    fun tickBaseline() {
        val current = _ui.value
        if (!current.baselineRunning) return
        val next = (current.baselineElapsedSec + 1).coerceAtMost(15)
        _ui.update { it.copy(baselineElapsedSec = next) }
        if (next >= 15) finishBaseline()
    }

    fun finishBaseline() {
        val sec = _ui.value.baselineElapsedSec.coerceAtLeast(1)
        _ui.update { it.copy(baselineRunning = false, baselineHoldSec = sec) }
        goNext()
    }

    fun goNext() {
        val current = _ui.value
        val next = when (current.step) {
            OnboardStep.WELCOME -> OnboardStep.DISCLAIMER
            OnboardStep.DISCLAIMER -> if (current.agreed) OnboardStep.AGE else return
            OnboardStep.AGE -> if (current.ageRange != null) OnboardStep.EXPERIENCE else return
            OnboardStep.EXPERIENCE -> if (current.experience != null) OnboardStep.GOALS else return
            OnboardStep.GOALS -> if (current.goals.isNotEmpty()) OnboardStep.BUDGET else return
            OnboardStep.BUDGET -> if (current.budget != null) OnboardStep.GUIDE else return
            OnboardStep.GUIDE -> if (current.guidePage < 2) {
                _ui.update { it.copy(guidePage = it.guidePage + 1) }
                return
            } else OnboardStep.BASELINE
            OnboardStep.BASELINE -> OnboardStep.PREVIEW
            OnboardStep.PREVIEW -> return
        }
        _ui.update { state ->
            val preview = if (next == OnboardStep.PREVIEW) previewPlan(state) else state.preview
            state.copy(step = next, preview = preview)
        }
    }

    fun goBack() {
        val current = _ui.value
        val prev = when (current.step) {
            OnboardStep.WELCOME -> return
            OnboardStep.DISCLAIMER -> OnboardStep.WELCOME
            OnboardStep.AGE -> OnboardStep.DISCLAIMER
            OnboardStep.EXPERIENCE -> OnboardStep.AGE
            OnboardStep.GOALS -> OnboardStep.EXPERIENCE
            OnboardStep.BUDGET -> OnboardStep.GOALS
            OnboardStep.GUIDE -> if (current.guidePage > 0) {
                _ui.update { it.copy(guidePage = it.guidePage - 1) }
                return
            } else OnboardStep.BUDGET
            OnboardStep.BASELINE -> OnboardStep.GUIDE
            OnboardStep.PREVIEW -> OnboardStep.BASELINE
        }
        _ui.update { it.copy(step = prev, baselineRunning = false) }
    }

    fun finish(onDone: () -> Unit) {
        val state = _ui.value
        val profile = state.toProfile() ?: return
        viewModelScope.launch {
            repo.completeOnboarding(profile)
            onDone()
        }
    }

    private fun previewPlan(state: OnboardUi): List<PlannedDay> {
        val profile = state.toProfile() ?: return emptyList()
        return PlanGenerator.generate(profile, WorkoutCatalog.all)
    }

    private fun OnboardUi.toProfile(): UserProfile? {
        return UserProfile(
            ageRange = ageRange ?: return null,
            experience = experience ?: return null,
            goals = if (goals.isEmpty()) return null else goals,
            dailyBudget = budget ?: return null,
            baselineHoldSec = baselineHoldSec,
        )
    }
}
