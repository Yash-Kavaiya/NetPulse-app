package com.example.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.common.ErrorBanner
import com.example.ui.common.LoadingState
import com.example.ui.common.OnResume
import com.example.ui.common.PermissionGate
import com.example.ui.components.AppDetailBottomSheet
import com.example.ui.components.AppItemCard
import com.example.ui.components.DataSummaryCard
import com.example.ui.components.FilterSection
import com.example.ui.theme.LocalGoogleColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val liveSpeed by viewModel.liveSpeed.collectAsStateWithLifecycle()
    val colors = LocalGoogleColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    OnResume { viewModel.checkPermissionAndLoad() }

    PermissionGate(uiState.hasPermission, onRecheck = viewModel::checkPermissionAndLoad) {
        if (uiState.isLoading) {
            LoadingState("Reading network statistics…")
            return@PermissionGate
        }
        val filtered = uiState.filteredApps
        val maxBytes = filtered.firstOrNull()?.getBytesForFilter(uiState.selectedNetworkFilter) ?: 1L
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("apps_usage_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary") {
                    DataSummaryCard(
                        summary = uiState.summary,
                        selectedFilter = uiState.selectedNetworkFilter,
                        timeRange = uiState.selectedTimeRange,
                        dataPlan = null,
                        liveSpeed = liveSpeed,
                        onEditDataPlan = {}
                    )
                }
                item(key = "filters") {
                    FilterSection(
                        selectedTimeRange = uiState.selectedTimeRange,
                        onTimeRangeSelected = viewModel::setTimeRange,
                        selectedNetworkFilter = uiState.selectedNetworkFilter,
                        onNetworkFilterSelected = viewModel::setNetworkFilter,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChanged = viewModel::setSearchQuery,
                        selectedSort = uiState.sortOption,
                        onSortSelected = viewModel::setSortOption
                    )
                }
                uiState.errorMessage?.let { msg ->
                    item(key = "error") { ErrorBanner(msg, onRetry = { viewModel.refresh() }) }
                }
                item(key = "header") {
                    Text(
                        text = "Apps with traffic (${filtered.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.darkGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.SearchOff, null, tint = colors.mediumGray, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No traffic found for these filters",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.mediumGray
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.uid }) { app ->
                        AppItemCard(
                            app = app,
                            selectedFilter = uiState.selectedNetworkFilter,
                            maxBytesInList = maxBytes,
                            onClick = { viewModel.loadAppDetail(app) }
                        )
                    }
                }
                item(key = "bottom") { Row(Modifier.height(24.dp)) {} }
            }
        }
    }

    if (uiState.selectedApp != null) {
        AppDetailBottomSheet(
            app = uiState.selectedApp,
            detail = uiState.selectedAppDetail,
            isLoading = uiState.isLoadingDetail,
            sheetState = sheetState,
            onDismiss = viewModel::dismissAppDetail,
            trend = uiState.selectedAppTrend
        )
    }
}
