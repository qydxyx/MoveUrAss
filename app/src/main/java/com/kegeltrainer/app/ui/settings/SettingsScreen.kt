package com.kegeltrainer.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Scaffold
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.AppTopBar
import com.kegeltrainer.app.ui.components.PrimaryButton
import com.kegeltrainer.app.ui.components.SelectChip
import com.kegeltrainer.app.ui.components.ScreenColumn
import com.kegeltrainer.app.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onArticle: (String) -> Unit,
    onDisclaimer: () -> Unit,
    onKnowledge: () -> Unit,
    onRedoOnboarding: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    var showTimePicker by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { vm.exportBackup(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { showImportConfirm = it }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
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
                SettingRow("息屏继续跟练", settings.backgroundPlayEnabled) { vm.setBackgroundPlay(it) }
            }
            Spacer(Modifier.height(16.dp))
            Text("每日提醒", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            AppCard {
                Text("通知只写「该做今日盆底训练了」，不含任何敏感健康描述。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.reminderMinutes.sorted().forEach { totalMinutes ->
                        val h = totalMinutes / 60
                        val m = totalMinutes % 60
                        val timeStr = String.format("%02d:%02d", h, m)
                        SelectChip(
                            label = "$timeStr ✕",
                            selected = true,
                            onClick = { vm.removeReminder(totalMinutes) },
                        )
                    }
                    SelectChip(
                        label = "+ 添加提醒",
                        selected = false,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showTimePicker = true
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("数据与备份", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            AppCard {
                Text("导出 JSON 备份文件可在换机或重装时完整恢复所有履历与设置。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            val defaultName = "moveurass-backup-${java.time.LocalDate.now()}.json"
                            exportLauncher.launch(defaultName)
                        },
                    ) {
                        Text("导出备份", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                    ) {
                        Text("恢复备份", color = MaterialTheme.colorScheme.secondary)
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
                "腺动  ${runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrDefault("1.2.0")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("所有健康训练数据 100% 离线保存在本机应用沙盒内。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 21, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("设置提醒时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    vm.addReminder(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            },
        )
    }

    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text("确认恢复数据？") },
            text = { Text("恢复备份将覆盖当前设备上的档案、计划及训练履历。此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.importBackup(context, uri)
                    showImportConfirm = null
                }) {
                    Text("确认恢复", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) {
                    Text("取消")
                }
            },
        )
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
    Scaffold(
        topBar = {
            AppTopBar(
                title = "健康声明",
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            DisclaimerBody()
            Spacer(Modifier.height(28.dp))
            PrimaryButton(text = "我知道了", onClick = onBack)
            Spacer(Modifier.height(16.dp))
        }
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
