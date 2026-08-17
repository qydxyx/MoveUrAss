package com.kegeltrainer.app.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kegeltrainer.app.domain.model.Level
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.WorkoutType
import com.kegeltrainer.app.domain.model.displayName
import com.kegeltrainer.app.domain.model.formatDuration
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.SelectChip
import com.kegeltrainer.app.ui.components.PrimaryButton
import com.kegeltrainer.app.ui.components.ScreenColumn


@Composable
fun LibraryScreen(
    onOpen: (String) -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("课程库", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        FilterRow(
            options = Level.entries.map { it to it.displayName() },
            selected = ui.level,
            onPick = vm::setLevel,
        )
        Spacer(Modifier.height(8.dp))
        FilterRow(
            options = WorkoutType.entries.map { it to it.displayName() },
            selected = ui.type,
            onPick = vm::setType,
        )
        Spacer(Modifier.height(16.dp))
        ui.items.forEach { workout ->
            AppCard(onClick = { onOpen(workout.id) }, modifier = Modifier.padding(bottom = 10.dp)) {
                Text(workout.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${workout.type.displayName()} · ${workout.level.displayName()} · ${formatDuration(workout.durationSec)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun <T> FilterRow(
    options: List<Pair<T, String>>,
    selected: T?,
    onPick: (T) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            SelectChip(label = label, selected = selected == value, onClick = { onPick(value) })
        }
    }
}

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onStart: (String) -> Unit,
    vm: WorkoutDetailViewModel = hiltViewModel(),
) {
    val workout = vm.workout
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(workout.type.displayName(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Text(workout.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "${workout.level.displayName()} · ${formatDuration(workout.durationSec)} · ${workout.contractionCount} 次收缩",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(workout.summary, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Text("阶段预览", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        workout.blocks.forEachIndexed { index, block ->
            val names = block.phases.joinToString(" → ") {
                when (it.type) {
                    PhaseType.CONTRACT -> "收 ${it.durationMs / 1000}s"
                    PhaseType.HOLD -> "持 ${it.durationMs / 1000}s"
                    PhaseType.RELAX -> "放 ${it.durationMs / 1000}s"
                    PhaseType.REST -> "歇 ${it.durationMs / 1000}s"
                    PhaseType.PREPARE -> "准备"
                }
            }
            Text("组 ${index + 1}  ×${block.repeat}    $names", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("注意", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        workout.coachTips.forEach {
            Text("· $it", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("开始训练", onClick = { onStart(workout.id) })
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("返回", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
