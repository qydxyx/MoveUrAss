package com.kegeltrainer.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kegeltrainer.app.data.backup.BackupManager
import com.kegeltrainer.app.data.prefs.AppSettings
import com.kegeltrainer.app.data.repo.TrainerRepository
import com.kegeltrainer.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: TrainerRepository,
    private val backupManager: BackupManager,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    val onboarded: StateFlow<Boolean?> = repo.settings
        .map<AppSettings, Boolean?> { it.onboarded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setVoice(value: Boolean) = viewModelScope.launch { repo.setVoice(value) }
    fun setCompletionSound(value: Boolean) = viewModelScope.launch { repo.setCompletionSound(value) }
    fun setHaptic(value: Boolean) = viewModelScope.launch { repo.setHaptic(value) }
    fun setStealth(value: Boolean) = viewModelScope.launch { repo.setStealth(value) }
    fun setBackgroundPlay(value: Boolean) = viewModelScope.launch { repo.setBackgroundPlay(value) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }

    fun addReminder(hour: Int, minute: Int) {
        val totalMinutes = (hour * 60 + minute).coerceIn(0, 1439)
        val current = settings.value.reminderMinutes.toMutableSet()
        if (current.add(totalMinutes)) {
            viewModelScope.launch {
                repo.setReminderMinutes(current)
            }
        }
    }

    fun removeReminder(totalMinutes: Int) {
        val current = settings.value.reminderMinutes.toMutableSet()
        if (current.remove(totalMinutes)) {
            viewModelScope.launch {
                repo.setReminderMinutes(current)
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = backupManager.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                _messages.emit("备份导出成功")
            } catch (e: Exception) {
                _messages.emit("导出失败：${e.message}")
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("无法读取所选备份文件")

                val result = backupManager.importJson(jsonString)
                if (result.isSuccess) {
                    _messages.emit("备份恢复成功")
                } else {
                    _messages.emit("恢复失败：${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _messages.emit("恢复失败：${e.message}")
            }
        }
    }
}
