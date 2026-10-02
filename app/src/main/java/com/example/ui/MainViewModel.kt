package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppDetailBreakdown
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.NetworkFilterType
import com.example.data.model.SortOption
import com.example.data.model.TimeRangeFilter
import com.example.data.room.AppDatabase
import com.example.data.room.DataPlanEntity
import com.example.data.room.DataPlanRepository
import com.example.data.stats.NetworkStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val summary: DeviceNetworkSummary = DeviceNetworkSummary(),
    val apps: List<AppNetworkUsage> = emptyList(),
    val selectedTimeRange: TimeRangeFilter = TimeRangeFilter.THIS_MONTH,
    val selectedNetworkFilter: NetworkFilterType = NetworkFilterType.ALL,
    val sortOption: SortOption = SortOption.TOTAL_DESC,
    val searchQuery: String = "",
    val liveSpeed: LiveTrafficSpeed = LiveTrafficSpeed(),
    val dataPlan: DataPlanEntity? = null,
    val selectedAppDetail: AppDetailBreakdown? = null,
    val isLoadingDetail: Boolean = false,
    val showDataPlanDialog: Boolean = false,
    val showArchInfoDialog: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredApps: List<AppNetworkUsage>
        get() {
            var list = apps

            // Search filter
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.appName.lowercase().contains(q) ||
                            (it.packageName?.lowercase()?.contains(q) == true) ||
                            (it.specialUidTag?.lowercase()?.contains(q) == true)
                }
            }

            // Network filter: filter out apps with 0 bytes for selected network
            list = list.filter { it.getBytesForFilter(selectedNetworkFilter) > 0 }

            // Sort
            return when (sortOption) {
                SortOption.TOTAL_DESC -> list.sortedByDescending { it.getBytesForFilter(selectedNetworkFilter) }
                SortOption.RX_DESC -> list.sortedByDescending { it.getRxBytesForFilter(selectedNetworkFilter) }
                SortOption.TX_DESC -> list.sortedByDescending { it.getTxBytesForFilter(selectedNetworkFilter) }
                SortOption.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
            }
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataPlanRepository = DataPlanRepository(AppDatabase.getDatabase(application).dataPlanDao())
    private val speedTracker = NetworkStatsHelper.RealtimeSpeedTracker()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Observe Data Plan from Room Database
        viewModelScope.launch {
            dataPlanRepository.dataPlan.collect { plan ->
                _uiState.update { it.copy(dataPlan = plan ?: DataPlanEntity()) }
            }
        }

        // Check permission and load initial data
        checkPermissionAndLoad()

        // Start real-time speed ticker
        startLiveSpeedTracker()
    }

    fun checkPermissionAndLoad() {
        val context = getApplication<Application>().applicationContext
        val permitted = NetworkStatsHelper.hasUsageAccessPermission(context)
        _uiState.update { it.copy(hasPermission = permitted) }

        if (permitted) {
            refreshData(showFullScreenLoading = _uiState.value.apps.isEmpty())
        }
    }

    fun refreshData(showFullScreenLoading: Boolean = false) {
        val context = getApplication<Application>().applicationContext
        val filter = _uiState.value.selectedTimeRange

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = showFullScreenLoading,
                    isRefreshing = !showFullScreenLoading,
                    errorMessage = null
                )
            }

            try {
                val (startTime, endTime) = NetworkStatsHelper.getTimeBounds(filter)

                val summary = withContext(Dispatchers.IO) {
                    NetworkStatsHelper.queryDeviceSummary(context, startTime, endTime)
                }

                val apps = withContext(Dispatchers.IO) {
                    NetworkStatsHelper.queryAppsSummary(context, startTime, endTime)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        summary = summary,
                        apps = apps
                    )
                }
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
        if (_uiState.value.selectedTimeRange != range) {
            _uiState.update { it.copy(selectedTimeRange = range) }
            refreshData(showFullScreenLoading = false)
        }
    }

    fun setNetworkFilter(filter: NetworkFilterType) {
        _uiState.update { it.copy(selectedNetworkFilter = filter) }
    }

    fun setSortOption(sort: SortOption) {
        _uiState.update { it.copy(sortOption = sort) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadAppDetail(app: AppNetworkUsage) {
        val context = getApplication<Application>().applicationContext
        val filter = _uiState.value.selectedTimeRange
        val (startTime, endTime) = NetworkStatsHelper.getTimeBounds(filter)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true) }

            val detail = withContext(Dispatchers.IO) {
                NetworkStatsHelper.queryUidDetails(
                    context = context,
                    uid = app.uid,
                    appName = app.appName,
                    packageName = app.packageName,
                    startTime = startTime,
                    endTime = endTime
                )
            }

            _uiState.update {
                it.copy(
                    isLoadingDetail = false,
                    selectedAppDetail = detail
                )
            }
        }
    }

    fun dismissAppDetail() {
        _uiState.update { it.copy(selectedAppDetail = null, isLoadingDetail = false) }
    }

    fun setShowDataPlanDialog(show: Boolean) {
        _uiState.update { it.copy(showDataPlanDialog = show) }
    }

    fun setShowArchInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showArchInfoDialog = show) }
    }

    fun saveDataPlan(limitBytes: Long, warningPercent: Int, isEnabled: Boolean, planType: String) {
        viewModelScope.launch {
            val updated = DataPlanEntity(
                id = 1,
                monthlyLimitBytes = limitBytes,
                warningPercent = warningPercent,
                isEnabled = isEnabled,
                planType = planType
            )
            dataPlanRepository.updatePlan(updated)
            _uiState.update { it.copy(showDataPlanDialog = false, dataPlan = updated) }
        }
    }

    private fun startLiveSpeedTracker() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val speed = speedTracker.sampleSpeed()
                _uiState.update { it.copy(liveSpeed = speed) }
                delay(1000L)
            }
        }
    }
}
