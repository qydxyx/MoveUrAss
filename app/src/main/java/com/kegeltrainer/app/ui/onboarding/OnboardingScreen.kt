package com.kegeltrainer.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kegeltrainer.app.domain.catalog.WorkoutCatalog
import com.kegeltrainer.app.domain.model.AgeRange
import com.kegeltrainer.app.domain.model.DailyBudget
import com.kegeltrainer.app.domain.model.Experience
import com.kegeltrainer.app.domain.model.Goal
import com.kegeltrainer.app.domain.model.displayName
import com.kegeltrainer.app.domain.model.formatDuration
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.SelectChip
import com.kegeltrainer.app.ui.components.PrimaryButton
import com.kegeltrainer.app.ui.components.ScreenColumn
import com.kegeltrainer.app.ui.settings.DisclaimerBody

import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.baselineRunning) {
        while (ui.baselineRunning) {
            delay(1000)
            vm.tickBaseline()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val progress = (ui.step.ordinal + 1f) / OnboardStep.entries.size
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
        )
        ScreenColumn(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            when (ui.step) {
                OnboardStep.WELCOME -> WelcomeStep()
                OnboardStep.DISCLAIMER -> DisclaimerStep(ui.agreed, vm::agree)
                OnboardStep.AGE -> AgeStep(ui.ageRange, vm::chooseAge)
                OnboardStep.EXPERIENCE -> ExperienceStep(ui.experience, vm::chooseExperience)
                OnboardStep.GOALS -> GoalsStep(ui.goals, vm::toggleGoal)
                OnboardStep.BUDGET -> BudgetStep(ui.budget, vm::chooseBudget)
                OnboardStep.GUIDE -> GuideStep(ui.guidePage)
                OnboardStep.BASELINE -> BaselineStep(ui)
                OnboardStep.PREVIEW -> PreviewStep(ui)
            }
        }
        Column(Modifier.padding(20.dp)) {
            val canContinue = when (ui.step) {
                OnboardStep.DISCLAIMER -> ui.agreed
                OnboardStep.AGE -> ui.ageRange != null
                OnboardStep.EXPERIENCE -> ui.experience != null
                OnboardStep.GOALS -> ui.goals.isNotEmpty()
                OnboardStep.BUDGET -> ui.budget != null
                OnboardStep.BASELINE -> !ui.baselineRunning
                else -> true
            }
            if (ui.step == OnboardStep.PREVIEW) {
                PrimaryButton("开始使用", onClick = { vm.finish(onFinished) })
            } else if (ui.step == OnboardStep.BASELINE && !ui.baselineRunning) {
                PrimaryButton("开始 15 秒测试", onClick = vm::startBaseline)
                TextButton(
                    onClick = vm::skipBaseline,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("跳过", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (ui.step == OnboardStep.BASELINE && ui.baselineRunning) {
                PrimaryButton("我松开了", onClick = vm::finishBaseline)
            } else {
                PrimaryButton(
                    text = if (ui.step == OnboardStep.GUIDE && ui.guidePage < 2) "下一页" else "继续",
                    onClick = vm::goNext,
                    enabled = canContinue,
                )
            }
            if (ui.step != OnboardStep.WELCOME) {
                TextButton(
                    onClick = vm::goBack,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("返回", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(32.dp))
    Text("腺动", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(8.dp))
    Text("男性盆底肌训练", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    Text(
        "用几分钟，按节奏学会正确收缩。数据只留在这台手机上，无需账号。",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun DisclaimerStep(agreed: Boolean, onAgree: (Boolean) -> Unit) {
    Text("使用前请了解", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    DisclaimerBody()
    Spacer(Modifier.height(8.dp))
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = agreed, onCheckedChange = onAgree)
        Text("我已阅读并了解以上说明", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgeStep(selected: AgeRange?, onChoose: (AgeRange) -> Unit) {
    Text("你的年龄段", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("用于安排起始强度，不会上传。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AgeRange.entries.forEach {
            SelectChip(label = it.displayName(), selected = selected == it, onClick = { onChoose(it) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExperienceStep(selected: Experience?, onChoose: (Experience) -> Unit) {
    Text("盆底训练经验", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Experience.entries.forEach {
            SelectChip(label = it.displayName(), selected = selected == it, onClick = { onChoose(it) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalsStep(selected: Set<Goal>, onToggle: (Goal) -> Unit) {
    Text("更想改善什么", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("可多选，今日主课会据此偏重。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Goal.entries.forEach {
            SelectChip(label = it.displayName(), selected = it in selected, onClick = { onToggle(it) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetStep(selected: DailyBudget?, onChoose: (DailyBudget) -> Unit) {
    Text("每天能拿出多久", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DailyBudget.entries.forEach {
            SelectChip(label = it.displayName(), selected = selected == it, onClick = { onChoose(it) })
        }
    }
}

@Composable
private fun GuideStep(page: Int) {
    val pages = listOf(
        "找到正确的肌肉" to "排尿中途试着减缓尿流，绷紧的就是盆底。找到后立刻停止——不要把中断排尿当成日常练习。也可以想象忍住排气，会阴轻轻上提。",
        "怎样算做对了" to "会阴与阴囊有轻微上提，腹、臀、大腿基本放松，并且能正常呼吸。小腹发硬、屁股夹紧或整个人憋气，都是代偿。",
        "入门体位" to "先仰卧屈膝，骨盆最放松。坐着、站着都可以练，但请先在最容易找准的体位上把感觉练稳。",
    )
    val (title, body) = pages[page]
    Text("找对盆底肌 ${page + 1}/3", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))
    Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(12.dp))
    Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun BaselineStep(ui: OnboardUi) {
    Text("可选：最长保持测试", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        "听到开始后收紧盆底并按住。松开即结束。上限 15 秒，用来微调计划，不是比赛。",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(24.dp))
    Text(
        if (ui.baselineRunning) "${ui.baselineElapsedSec} 秒" else "准备好再开始",
        color = if (ui.baselineRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.displayLarge,
    )
    if (ui.baselineRunning) {
        Spacer(Modifier.height(8.dp))
        Text("保持收缩，不要憋气", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewStep(ui: OnboardUi) {
    Text("你的 28 天计划已就绪", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text("今日主课按此表推送。课程库随时可以加练。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
    ui.preview.chunked(7).forEachIndexed { week, days ->
        AppCard(modifier = Modifier.padding(bottom = 10.dp)) {
            Text("第 ${week + 1} 周", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            days.forEach { day ->
                val workout = WorkoutCatalog.find(day.workoutId)
                Text(
                    "第 ${day.dayIndex + 1} 天  ${workout?.title ?: ""}  ${workout?.let { formatDuration(it.durationSec) } ?: ""}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
