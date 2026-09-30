package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UsageBucket
import com.example.data.room.DailyUsageDao
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryRange(val days: Int, val label: String) {
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
    QUARTER(90, "90 days")
}

data class HistoryUiState(
    val hasPermission: Boolean = true,
    val isLoading: Boolean = true,
    val range: HistoryRange = HistoryRange.WEEK,
    val days: List<UsageBucket> = emptyList(),
    val previousTotal: Long = 0L,
    val selectedDayIndex: Int? = null,
    val hourly: List<UsageBucket> = emptyList(),
    val isLoadingHourly: Boolean = false
) {
    val total: Long get() = days.sumOf { it.totalBytes }
    val mobileTotal: Long get() = days.sumOf { it.mobileBytes }
    val wifiTotal: Long get() = days.sumOf { it.wifiBytes }
    val dailyAverage: Long get() = if (days.isEmpty()) 0L else total / days.size
    val peakDay: UsageBucket? get() = days.maxByOrNull { it.totalBytes }

    /** Percent change vs. the preceding period of equal length, or null without history. */
    val changePercent: Int?
        get() = if (previousTotal <= 0L) null else (((total - previousTotal) * 100) / previousTotal).toInt()
}

object HistoryMerge {
    /**
     * Prefers live NetworkStats buckets, falling back to persisted daily snapshots for days the
     * platform no longer retains (it reports zero for them).
     */
    fun merge(live: List<UsageBucket>, snapshots: Map<Long, UsageBucket>): List<UsageBucket> =
        live.map { bucket ->
            if (bucket.totalBytes == 0L) snapshots[bucket.startTime] ?: bucket else bucket
        }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val stats: UsageStatsSource,
    private val dailyUsageDao: DailyUsageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var hourlyJob: Job? = null

    fun checkPermissionAndLoad() {
        val permitted = stats.hasPermission()
        _uiState.update { it.copy(hasPermission = permitted) }
        if (permitted) load()
    }

    fun setRange(range: HistoryRange) {
        if (range == _uiState.value.range) return
        _uiState.update { it.copy(range = range, selectedDayIndex = null, hourly = emptyList()) }
        load()
    }

    fun selectDay(index: Int) {
        val day = _uiState.value.days.getOrNull(index) ?: return
        if (_uiState.value.selectedDayIndex == index) {
            _uiState.update { it.copy(selectedDayIndex = null, hourly = emptyList()) }
            return
        }
        _uiState.update { it.copy(selectedDayIndex = index, isLoadingHourly = true) }
        hourlyJob?.cancel()
        hourlyJob = viewModelScope.launch {
            val hours = (0 until 24).map { day.startTime + it * 3_600_000L }.filter { it < day.endTime }
            val hourly = stats.deviceBuckets(hours, day.endTime)
            _uiState.update { it.copy(hourly = hourly, isLoadingHourly = false) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.days.isEmpty()) }
            val range = _uiState.value.range
            val now = System.currentTimeMillis()
            val today = TimeRanges.startOfDay(now)
            val start = TimeRanges.addDays(today, -(range.days - 1))
            val prevStart = TimeRanges.addDays(start, -range.days)

            val dayStarts = TimeRanges.dayStarts(start, now)
            val snapshots = dailyUsageDao.observeSince(prevStart).first()
                .associate { it.dayStart to UsageBucket(it.dayStart, it.dayStart + TimeRanges.DAY_MS, it.mobileBytes, it.wifiBytes) }
            val days = HistoryMerge.merge(stats.deviceBuckets(dayStarts, now), snapshots)

            // Previous period: platform total, or the snapshot sum if the platform has dropped it.
            val prevLive = stats.deviceSummary(prevStart, start).grandTotalBytes
            val prevSnap = snapshots.filterKeys { it in prevStart until start }.values.sumOf { it.totalBytes }
            _uiState.update {
                it.copy(isLoading = false, days = days, previousTotal = maxOf(prevLive, prevSnap))
            }
        }
    }
}
