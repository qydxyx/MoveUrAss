package com.kegeltrainer.app.ui.player

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.displayLabel
import com.kegeltrainer.app.domain.model.formatDuration
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.PrimaryButton
import com.kegeltrainer.app.ui.theme.Amber
import com.kegeltrainer.app.ui.theme.Ink
import com.kegeltrainer.app.ui.theme.InkMuted
import com.kegeltrainer.app.ui.theme.Line
import com.kegeltrainer.app.ui.theme.Night
import com.kegeltrainer.app.ui.theme.Teal

@Composable
fun PlayerScreen(
    onExit: () -> Unit,
    vm: PlayerViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.onBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler {
        if (ui.finished) onExit() else vm.requestExit()
    }
    if (ui.finished) {
        CompletePane(ui = ui, onDone = { vm.leaveFinished(onExit) })
        return
    }
    val phase = ui.snapshot.phase
    val targetColor = when (phase.type) {
        PhaseType.CONTRACT, PhaseType.HOLD -> Amber
        PhaseType.RELAX -> Teal
        PhaseType.PREPARE -> Ink
        PhaseType.REST -> InkMuted
    }
    val color by animateColorAsState(targetColor, tween(400), label = "phaseColor")
    val targetScale = when (phase.type) {
        PhaseType.CONTRACT -> 0.72f - (phase.intensity - 1) * 0.04f
        PhaseType.HOLD -> 0.68f - (phase.intensity - 1) * 0.03f
        PhaseType.RELAX, PhaseType.REST -> 1f
        PhaseType.PREPARE -> 0.9f
    }
    val scale by animateFloatAsState(targetScale, tween(700), label = "ring")
    val dim = if (ui.stealth) 0.28f else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding()
            .navigationBarsPadding()
            .graphicsLayer { alpha = dim },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = vm::requestExit) { Text("结束", color = InkMuted) }
                Text(ui.workout.title, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Row {
                    IconToggle(Icons.Outlined.GraphicEq, ui.voice, vm::toggleVoice)
                    IconToggle(Icons.Outlined.Vibration, ui.haptic, vm::toggleHaptic)
                    IconToggle(Icons.Outlined.VisibilityOff, ui.stealth, vm::toggleStealth)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
                Canvas(Modifier.size(280.dp)) {
                    drawCircle(
                        color = color.copy(alpha = 0.12f),
                        radius = size.minDimension / 2f,
                    )
                    drawCircle(
                        color = color,
                        radius = (size.minDimension / 2f) * scale,
                        style = Stroke(width = 18.dp.toPx()),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(phase.type.displayLabel(), color = color, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${ui.snapshot.remainingSec}",
                        color = Ink,
                        style = MaterialTheme.typography.displayLarge,
                    )
                    if (phase.intensity > 1) {
                        Text("强度 ${phase.intensity}", color = InkMuted)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "收缩  ${ui.snapshot.contractionIndex} / ${ui.snapshot.contractionTotal}",
                color = InkMuted,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { ui.snapshot.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = Line,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = vm::pauseOrResume,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
            ) {
                Icon(
                    imageVector = if (ui.snapshot.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    contentDescription = if (ui.snapshot.isPaused) "继续" else "暂停",
                    tint = color,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (ui.confirmExit) {
        AlertDialog(
            onDismissRequest = vm::dismissExit,
            title = { Text("结束这次训练？") },
            text = { Text("未完成的课程会记入履历，但不点亮今日主课。") },
            confirmButton = {
                TextButton(onClick = { vm.confirmExit(onExit) }) { Text("结束") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissExit) { Text("继续练") }
            },
        )
    }
}

@Composable
private fun CompletePane(ui: PlayerUi, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("完成", color = Teal, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(ui.workout.title, color = Ink, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        AppCard {
            Text("用时 ${formatDuration((ui.snapshot.elapsedMs / 1000).toInt())}", color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("收缩 ${ui.snapshot.contractionTotal} 次", color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("已写入今日履历", color = InkMuted)
        }
        Spacer(Modifier.height(16.dp))
        AppCard {
            Text("课后提示", color = Teal, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(ui.tip, color = Ink, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("回到今日", onClick = onDone)
    }
}

@Composable
private fun IconToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    on: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = if (on) Teal else InkMuted)
    }
}
