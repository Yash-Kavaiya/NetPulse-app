package com.example.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.Csv
import com.example.data.export.ExportManager
import com.example.data.model.TimeRangeFilter
import com.example.data.plan.PlanRepository
import com.example.data.prefs.AppSettings
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.ThemeMode
import com.example.data.room.DataPlanEntity
import com.example.data.room.SpeedTestDao
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import com.example.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExportKind(val fileName: String, val mimeType: String) {
    DAILY_CSV("netpulse-daily-usage.csv", "text/csv"),
    APPS_CSV("netpulse-app-usage.csv", "text/csv"),
    SPEED_CSV("netpulse-speed-tests.csv", "text/csv"),
    BACKUP("netpulse-backup.json", "application/json")
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val planRepository: PlanRepository,
    private val exportManager: ExportManager,
    private val stats: UsageStatsSource,
    private val speedTestDao: SpeedTestDao
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val plan: StateFlow<DataPlanEntity> = planRepository.plan
        .stateIn(viewModelScope, SharingStarted.Eagerly, DataPlanEntity())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }

    fun setAlerts(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAlertsEnabled(enabled)
        if (enabled) WorkScheduler.runNow(context)
    }

    fun savePlan(plan: DataPlanEntity) = viewModelScope.launch {
        planRepository.save(plan)
        WorkScheduler.runNow(context)
        _messages.send("Data plan saved")
    }

    fun export(kind: ExportKind, uri: Uri) = viewModelScope.launch {
        try {
            when (kind) {
                ExportKind.BACKUP -> exportManager.exportBackup(uri)
                ExportKind.DAILY_CSV -> {
                    val now = System.currentTimeMillis()
                    val days = TimeRanges.dayStarts(TimeRanges.addDays(TimeRanges.startOfDay(now), -89), now)
                    exportManager.writeText(uri, Csv.dailyUsage(stats.deviceBuckets(days, now)))
                }
                ExportKind.APPS_CSV -> {
                    val (start, end) = TimeRanges.bounds(TimeRangeFilter.BILLING_CYCLE, planRepository.current().cycleStartDay)
                    exportManager.writeText(uri, Csv.apps(stats.apps(start, end)))
                }
                ExportKind.SPEED_CSV -> exportManager.writeText(uri, Csv.speedTests(speedTestDao.getAll()))
            }
            _messages.send("Exported ${kind.fileName}")
        } catch (e: Exception) {
            _messages.send("Export failed: ${e.localizedMessage}")
        }
    }

    fun restore(uri: Uri) = viewModelScope.launch {
        try {
            exportManager.importBackup(uri)
            _messages.send("Backup restored")
        } catch (e: Exception) {
            _messages.send("Restore failed: ${e.localizedMessage ?: "invalid file"}")
        }
    }
}
