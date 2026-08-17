package com.kegeltrainer.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kegeltrainer.app.domain.model.formatDuration
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.ScreenColumn
import com.kegeltrainer.app.ui.components.StatTile

import java.time.YearMonth

@Composable
fun ProgressScreen(vm: ProgressViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("履历", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("完成次数", "${ui.totalCount}", Modifier.weight(1f))
            StatTile("累计分钟", "${ui.totalMinutes}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("当前连续", "${ui.streak} 天", Modifier.weight(1f))
            StatTile("最长连续", "${ui.longest} 天", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("${ui.month.year} 年 ${ui.month.monthValue} 月", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        MonthGrid(ui.month, ui.daysOn)
        Spacer(Modifier.height(20.dp))
        Text("成就", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        ui.achievements.forEach { (def, on) ->
            AppCard(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(def.title, color = if (on) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                Text(if (on) def.detail else "未解锁 · ${def.detail}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("最近记录", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        if (ui.recent.isEmpty()) {
            Text("还没有训练记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ui.recent.forEach { session ->
                AppCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(vm.titleOf(session.workoutId), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${session.dayLabel()} · ${formatDuration((session.durationMs / 1000).toInt())} · ${if (session.completed) "完成" else "未完成"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, daysOn: Set<Int>) {
    val firstDow = month.atDay(1).dayOfWeek.value // 1=Mon
    val days = month.lengthOfMonth()
    val cells = buildList {
        repeat(firstDow - 1) { add(null) }
        repeat(days) { add(it + 1) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    val on = day != null && day in daysOn
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            Text(
                                "$day",
                                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                repeat(7 - week.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
