package com.example.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.InsightInput
import com.example.data.ai.InsightResult
import com.example.data.ai.InsightsRepository
import com.example.data.model.TimeRangeFilter
import com.example.data.plan.PlanRepository
import com.example.data.room.InsightEntity
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val isConfigured: Boolean = true,
    val hasPermission: Boolean = true,
    val isGenerating: Boolean = false,
    val range: TimeRangeFilter = TimeRangeFilter.LAST_7_DAYS,
    val error: String? = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insights: InsightsRepository,
    private val stats: UsageStatsSource,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InsightsUiState(isConfigured = insights.isConfigured(), hasPermission = stats.hasPermission())
    )
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    val latest: StateFlow<InsightEntity?> = insights.cached
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun recheck() = _uiState.update { it.copy(hasPermission = stats.hasPermission(), isConfigured = insights.isConfigured()) }

    fun setRange(range: TimeRangeFilter) = _uiState.update { it.copy(range = range) }

    fun generate() {
        if (_uiState.value.isGenerating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            val range = _uiState.value.range
            val plan = planRepository.current()
            val (start, end) = TimeRanges.bounds(range, plan.cycleStartDay)
            val input = InsightInput(
                rangeLabel = range.label,
                summary = stats.deviceSummary(start, end),
                topApps = stats.apps(start, end).take(10),
                cycle = if (plan.isEnabled) planRepository.cycleUsage() else null
            )
            when (val result = insights.generate(input)) {
                is InsightResult.Success -> _uiState.update { it.copy(isGenerating = false) }
                InsightResult.NotConfigured -> _uiState.update { it.copy(isGenerating = false, isConfigured = false) }
                is InsightResult.Error -> _uiState.update { it.copy(isGenerating = false, error = result.message) }
            }
        }
    }
}
