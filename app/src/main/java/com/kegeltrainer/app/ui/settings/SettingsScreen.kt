package com.kegeltrainer.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.SelectChip
import com.kegeltrainer.app.ui.components.ScreenColumn
import com.kegeltrainer.app.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onArticle: (String) -> Unit,
    onDisclaimer: () -> Unit,
    onKnowledge: () -> Unit,
    onRedoOnboarding: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val activity = LocalContext.current as android.app.Activity
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("我的", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text("外观", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        AppCard {
            Text("浅色与深色可跟随系统，也可单独指定。深色为 OLED 纯黑。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    SelectChip(
                        label = mode.displayName(),
                        selected = settings.themeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        AppCard {
            SettingRow("语音引导", settings.voiceEnabled) { vm.setVoice(it) }
            SettingRow("震动引导", settings.hapticEnabled) { vm.setHaptic(it) }
            SettingRow("隐蔽模式", settings.stealthEnabled) { vm.setStealth(it) }
        }
        Spacer(Modifier.height(16.dp))
        Text("每日提醒", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        AppCard {
            Text("通知只写「该做今日盆底训练了」，不含更细的健康描述。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8 to "08:00", 12 to "12:00", 21 to "21:00").forEach { (hour, label) ->
                    SelectChip(
                        label = label,
                        selected = hour in settings.reminderHours,
                        onClick = { vm.toggleReminder(hour, activity) },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        AppCard(onClick = onKnowledge) {
            Text("知识库", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Text("找肌、常见错误、何时停练", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        AppCard(onClick = onDisclaimer) {
            Text("健康声明", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Text("可随时再读", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        AppCard(onClick = onRedoOnboarding) {
            Text("重新做问卷", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            Text("会生成新的 28 天计划，已有履历保留", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "腺动  ${activity.packageManager.getPackageInfo(activity.packageName, 0).versionName}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("训练数据仅保存在本机。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun DisclaimerScreen(onBack: () -> Unit) {
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("健康声明", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        DisclaimerBody()
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("返回", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun DisclaimerBody() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "腺动是盆底肌训练的节奏引导工具，不是医疗器械，也不能诊断或治疗任何疾病。",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "凯格尔运动可能有助于膀胱控制与盆底力量，部分人会感到舒适度改善。它不能替代医师对前列腺炎、增生、术后康复或其他泌尿问题的诊疗。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "出现发热、血尿、急性尿潴留、会阴剧痛、术后未经医生允许等情况，请停止自行训练并就医。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "所有训练记录只保存在这台设备上。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
