package com.kegeltrainer.app.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.kegeltrainer.app.domain.model.displayName
import com.kegeltrainer.app.domain.model.formatDuration
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.PrimaryButton
import com.kegeltrainer.app.ui.components.ScreenColumn
import com.kegeltrainer.app.ui.components.WeekDots


@Composable
fun HomeScreen(
    onStart: (workoutId: String, plan: Boolean) -> Unit,
    onDetail: (workoutId: String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("今日", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("腺动", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        AppCard {
            Text(
                if (ui.streak > 0) "已连续 ${ui.streak} 天" else "从今天开始连续",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text("每天几分钟，比一次练崩更有用。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
        val workout = ui.workout
        if (workout != null) {
            AppCard(onClick = { onDetail(workout.id) }) {
                Text(
                    if (ui.planDone) "今日主课已完成" else "第 ${ui.dayIndex + 1} 天 · 主课",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(workout.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${workout.type.displayName()} · ${workout.level.displayName()} · ${formatDuration(workout.durationSec)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(workout.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = if (ui.planDone) "再练一次" else "开始今日课",
                    onClick = { onStart(workout.id, true) },
                )
            }
        } else {
            AppCard {
                Text("还没有生成计划", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                Text("请先完成入门问卷。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("本周", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        WeekDots(ui.weekFlags)
        Spacer(Modifier.height(20.dp))
        AppCard {
            Text("今日提示", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(ui.tip, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
