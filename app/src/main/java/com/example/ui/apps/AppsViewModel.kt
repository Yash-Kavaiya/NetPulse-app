package com.example.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppDetailBreakdown
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.NetworkFilterType
import com.example.data.model.SortOption
import com.example.data.model.TimeRangeFilter
import com.example.data.model.UsageBucket
import com.example.data.plan.PlanRepository
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsUiState(
    val hasPermission: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val summary: DeviceNetworkSummary = DeviceNetworkSummary(),
    val apps: List<AppNetworkUsage> = emptyList(),
    val selectedTimeRange: TimeRangeFilter = TimeRangeFilter.BILLING_CYCLE,
    val selectedNetworkFilter: NetworkFilterType = NetworkFilterType.ALL,
    val sortOption: SortOption = SortOption.TOTAL_DESC,
    val searchQuery: String = "",
    val selectedApp: AppNetworkUsage? = null,
    val selectedAppDetail: AppDetailBreakdown? = null,
    val selectedAppTrend: List<UsageBucket> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredApps: List<AppNetworkUsage>
        get() = filterAndSort(apps, searchQuery, selectedNetworkFilter, sortOption)

    companion object {
        fun filterAndSort(
            apps: List<AppNetworkUsage>,
            searchQuery: String,
            filter: NetworkFilterType,
            sort: SortOption
        ): List<AppNetworkUsage> {
            var list = apps
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.appName.lowercase().contains(q) ||
                        (it.packageName?.lowercase()?.contains(q) == true) ||
                        (it.specialUidTag?.lowercase()?.contains(q) == true)
                }
            }
            // Hide apps with no traffic on the selected network.
            list = list.filter { it.getBytesForFilter(filter) > 0 }
            return when (sort) {
                SortOption.TOTAL_DESC -> list.sortedByDescending { it.getBytesForFilter(filter) }
                SortOption.RX_DESC -> list.sortedByDescending { it.getRxBytesForFilter(filter) }
                SortOption.TX_DESC -> list.sortedByDescending { it.getTxBytesForFilter(filter) }
                SortOption.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
            }
        }
    }
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val stats: UsageStatsSource,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    val liveSpeed: StateFlow<LiveTrafficSpeed> = stats.liveSpeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveTrafficSpeed())

    private var loadJob: Job? = null

    fun checkPermissionAndLoad() {
        val permitted = stats.hasPermission()
        _uiState.update { it.copy(hasPermission = permitted) }
        if (permitted) refresh(showFullScreenLoading = _uiState.value.apps.isEmpty())
    }

    fun refresh(showFullScreenLoading: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = showFullScreenLoading, isRefreshing = !showFullScreenLoading, errorMessage = null)
            }
            try {
                val (start, end) = bounds()
                val summary = stats.deviceSummary(start, end)
                val apps = stats.apps(start, end)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, summary = summary, apps = apps) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Failed to read network statistics"
                    )
                }
            }
        }
    }

    fun setTimeRange(range: TimeRangeFilter) {
        if (_uiState.value.selectedTimeRange == range) return
        _uiState.update { it.copy(selectedTimeRange = range) }
        refresh()
    }

    fun setNetworkFilter(filter: NetworkFilterType) = _uiState.update { it.copy(selectedNetworkFilter = filter) }
    fun setSortOption(sort: SortOption) = _uiState.update { it.copy(sortOption = sort) }
    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun loadAppDetail(app: AppNetworkUsage) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedApp = app, isLoadingDetail = true, selectedAppDetail = null, selectedAppTrend = emptyList()) }
            val (start, end) = bounds()
            val detail = stats.appDetail(app, start, end)
            val now = System.currentTimeMillis()
            val days = TimeRanges.dayStarts(TimeRanges.addDays(TimeRanges.startOfDay(now), -13), now)
            val trend = stats.appDailyBuckets(app.uid, days, now)
            _uiState.update { it.copy(isLoadingDetail = false, selectedAppDetail = detail, selectedAppTrend = trend) }
        }
    }

    fun dismissAppDetail() =
        _uiState.update { it.copy(selectedApp = null, selectedAppDetail = null, selectedAppTrend = emptyList(), isLoadingDetail = false) }

    private suspend fun bounds(): Pair<Long, Long> =
        TimeRanges.bounds(_uiState.value.selectedTimeRange, planRepository.current().cycleStartDay)
}
