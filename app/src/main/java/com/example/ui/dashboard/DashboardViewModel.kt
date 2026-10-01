package com.example.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.UsageBucket
import com.example.data.plan.CycleUsage
import com.example.data.plan.PlanRepository
import com.example.data.room.DataPlanEntity
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import com.example.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val hasPermission: Boolean = true,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val today: DeviceNetworkSummary = DeviceNetworkSummary(),
    val yesterday: DeviceNetworkSummary = DeviceNetworkSummary(),
    val cycleSummary: DeviceNetworkSummary = DeviceNetworkSummary(),
    val cycle: CycleUsage? = null,
    val topApps: List<AppNetworkUsage> = emptyList(),
    /** Daily totals for the last 7 days, oldest first; the last entry is today. */
    val week: List<UsageBucket> = emptyList(),
    val showPlanDialog: Boolean = false,
    val errorMessage: String? = null
) {
    /** Projected cycle usage if the current daily pace continues until the cycle resets. */
    val projectedCycleBytes: Long?
        get() {
            val c = cycle ?: return null
            val now = System.currentTimeMillis()
            val elapsed = (now - c.cycleStart).coerceAtLeast(TimeRanges.DAY_MS / 4)
            val length = (c.cycleEnd - c.cycleStart).coerceAtLeast(1)
            return (c.usedBytes.toDouble() / elapsed * length).toLong()
        }

    val tips: List<UsageTip>
        get() = UsageTips.build(today, yesterday, cycleSummary, cycle, projectedCycleBytes, topApps)
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stats: UsageStatsSource,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val liveSpeed: StateFlow<LiveTrafficSpeed> = stats.liveSpeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveTrafficSpeed())

    val plan: StateFlow<DataPlanEntity> = planRepository.plan
        .stateIn(viewModelScope, SharingStarted.Eagerly, DataPlanEntity())

    private var loadJob: Job? = null

    fun checkPermissionAndLoad() {
        val permitted = stats.hasPermission()
        _uiState.update { it.copy(hasPermission = permitted) }
        if (permitted) refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = !it.isLoading, errorMessage = null) }
            try {
                val now = System.currentTimeMillis()
                val startToday = TimeRanges.startOfDay(now)
                val cycleDeferred = async { planRepository.cycleUsage(now) }
                val today = async { stats.deviceSummary(startToday, now) }
                val yesterday = async { stats.deviceSummary(TimeRanges.addDays(startToday, -1), startToday) }
                val cycle = cycleDeferred.await()
                val cycleSummary = async { stats.deviceSummary(cycle.cycleStart, now) }
                val apps = async { stats.apps(cycle.cycleStart, now).take(5) }
                val week = async {
                    stats.deviceBuckets(TimeRanges.dayStarts(TimeRanges.addDays(startToday, -6), now), now)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        today = today.await(),
                        yesterday = yesterday.await(),
                        cycle = cycle,
                        cycleSummary = cycleSummary.await(),
                        topApps = apps.await(),
                        week = week.await()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, errorMessage = e.localizedMessage ?: "Failed to load usage")
                }
            }
        }
    }

    fun showPlanDialog(show: Boolean) = _uiState.update { it.copy(showPlanDialog = show) }

    fun savePlan(plan: DataPlanEntity) {
        viewModelScope.launch {
            planRepository.save(plan)
            _uiState.update { it.copy(showPlanDialog = false) }
            WorkScheduler.runNow(context)
            refresh()
        }
    }
}
