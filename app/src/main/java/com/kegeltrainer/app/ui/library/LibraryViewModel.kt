package com.kegeltrainer.app.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.Level
import com.kegeltrainer.app.domain.model.Workout
import com.kegeltrainer.app.domain.model.WorkoutType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class LibraryUi(
    val level: Level? = null,
    val type: WorkoutType? = null,
    val items: List<Workout> = WorkoutCatalog.all,
)

@HiltViewModel
class LibraryViewModel @Inject constructor() : ViewModel() {
    private val _ui = MutableStateFlow(LibraryUi())
    val ui: StateFlow<LibraryUi> = _ui

    fun setLevel(level: Level?) {
        _ui.update { it.copy(level = if (it.level == level) null else level).filtered() }
    }

    fun setType(type: WorkoutType?) {
        _ui.update { it.copy(type = if (it.type == type) null else type).filtered() }
    }

    private fun LibraryUi.filtered(): LibraryUi = copy(
        items = WorkoutCatalog.all.filter { workout ->
            (level == null || workout.level == level) &&
                (type == null || workout.type == type)
        },
    )
}

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val workout: Workout = WorkoutCatalog.byId(checkNotNull(savedStateHandle["workoutId"]))
}
