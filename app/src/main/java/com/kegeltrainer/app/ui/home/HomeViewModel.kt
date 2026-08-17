package com.kegeltrainer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.domain.achievements.Achievements
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.knowledge.KnowledgeArticles
import com.kegeltrainer.app.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUi(
    val streak: Int = 0,
    val workout: Workout? = null,
    val dayIndex: Int = 0,
    val planDone: Boolean = false,
    val weekFlags: List<Boolean> = List(7) { false },
    val tip: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repo: TrainerRepository,
) : ViewModel() {
    val ui = combine(repo.todayState, repo.sessions) { today, sessions ->
        val workout = today.planned?.let { WorkoutCatalog.find(it.workoutId) }
        val weekFlags = weekCompletion(sessions)
        HomeUi(
            streak = Achievements.currentStreak(sessions),
            workout = workout,
            dayIndex = today.dayIndex,
            planDone = today.planCompletedToday,
            weekFlags = weekFlags,
            tip = KnowledgeArticles.rotatingTips[
                (LocalDate.now().toEpochDay().toInt()).mod(KnowledgeArticles.rotatingTips.size),
            ],
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())
}

private fun weekCompletion(sessions: List<com.kegeltrainer.app.domain.model.SessionRecord>): List<Boolean> {
    val today = LocalDate.now()
    val monday = today.with(DayOfWeek.MONDAY)
    val doneDays = sessions.filter { it.completed }.map {
        Instant.ofEpochMilli(it.endedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSet()
    return (0..6).map { monday.plusDays(it.toLong()) in doneDays }
}
