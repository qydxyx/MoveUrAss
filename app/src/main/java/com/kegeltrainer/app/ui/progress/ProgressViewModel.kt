package com.kegeltrainer.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.domain.achievements.AchievementDef
import com.kegeltrainer.app.domain.achievements.Achievements
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.SessionRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProgressUi(
    val month: YearMonth = YearMonth.now(),
    val daysOn: Set<Int> = emptySet(),
    val totalCount: Int = 0,
    val totalMinutes: Int = 0,
    val streak: Int = 0,
    val longest: Int = 0,
    val achievements: List<Pair<AchievementDef, Boolean>> = emptyList(),
    val recent: List<SessionRecord> = emptyList(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    repo: TrainerRepository,
) : ViewModel() {
    val ui = repo.sessions.map { sessions ->
        val done = sessions.filter { it.completed }
        val month = YearMonth.now()
        val daysOn = done.mapNotNull { session ->
            val date = Instant.ofEpochMilli(session.endedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            if (YearMonth.from(date) == month) date.dayOfMonth else null
        }.toSet()
        val unlocked = Achievements.unlockedIds(sessions)
        ProgressUi(
            month = month,
            daysOn = daysOn,
            totalCount = done.size,
            totalMinutes = (done.sumOf { it.durationMs } / 60_000L).toInt(),
            streak = Achievements.currentStreak(sessions),
            longest = Achievements.longestStreak(sessions),
            achievements = Achievements.all.map { it to (it.id in unlocked) },
            recent = sessions.take(20),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUi())

    fun titleOf(id: String) = WorkoutCatalog.find(id)?.title ?: id
}

fun SessionRecord.dayLabel(): String {
    val date = Instant.ofEpochMilli(endedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (date == LocalDate.now()) "今天" else date.toString()
}
