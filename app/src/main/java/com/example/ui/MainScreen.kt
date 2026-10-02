package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.formatByteSize
import com.example.ui.components.AppDetailBottomSheet
import com.example.ui.components.AppItemCard
import com.example.ui.components.ArchitectureInfoDialog
import com.example.ui.components.DataPlanDialog
import com.example.ui.components.DataSummaryCard
import com.example.ui.components.FilterSection
import com.example.ui.components.PermissionRequiredCard
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleUIBlue
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.LocalGoogleColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val googleColors = LocalGoogleColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Recheck permission and refresh data whenever app resumes from background (e.g., after visiting Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Google 4-color dot cluster
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoogleBlue))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoogleRed))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoogleYellow))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoogleGreen))
                        }
                        Text(
                            text = "NetPulse",
                            fontWeight = FontWeight.Bold,
                            color = googleColors.darkGray,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Educational Info Dialog (Architecture & Methodologies)
                    IconButton(
                        onClick = { viewModel.setShowArchInfoDialog(true) },
                        modifier = Modifier.testTag("architecture_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Architecture Info",
                            tint = GoogleUIBlue
                        )
                    }

                    // Data Budget Dialog
                    IconButton(
                        onClick = { viewModel.setShowDataPlanDialog(true) },
                        modifier = Modifier.testTag("data_budget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataUsage,
                            contentDescription = "Data Budget",
                            tint = GoogleUIBlue
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.refreshData(showFullScreenLoading = false) },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = GoogleUIBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.hasPermission) {
                // Permission Onboarding Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionRequiredCard(
                        onRecheckPermission = { viewModel.checkPermissionAndLoad() }
                    )
                }
            } else if (uiState.isLoading) {
                // Loading Fullscreen Spinner
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = GoogleUIBlue,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aggregating NetworkStats Buckets...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = googleColors.mediumGray
                        )
                    }
                }
            } else {
                // Main Content List
                val filteredList = uiState.filteredApps
                val maxBytesInList = filteredList.firstOrNull()?.getBytesForFilter(uiState.selectedNetworkFilter) ?: 1L

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("apps_usage_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item 1: Device Summary Card
                    item(key = "summary_card") {
                        DataSummaryCard(
                            summary = uiState.summary,
                            selectedFilter = uiState.selectedNetworkFilter,
                            timeRange = uiState.selectedTimeRange,
                            dataPlan = uiState.dataPlan,
                            liveSpeed = uiState.liveSpeed,
                            onEditDataPlan = { viewModel.setShowDataPlanDialog(true) }
                        )
                    }

                    // Item 2: Filter and Search Section
                    item(key = "filters_section") {
                        FilterSection(
                            selectedTimeRange = uiState.selectedTimeRange,
                            onTimeRangeSelected = { viewModel.setTimeRange(it) },
                            selectedNetworkFilter = uiState.selectedNetworkFilter,
                            onNetworkFilterSelected = { viewModel.setNetworkFilter(it) },
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            selectedSort = uiState.sortOption,
                            onSortSelected = { viewModel.setSortOption(it) }
                        )
                    }

                    // Item 3: Count and Total Header
                    item(key = "list_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Apps with Traffic (${filteredList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = googleColors.darkGray
                            )

                            if (uiState.isRefreshing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = GoogleUIBlue,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Syncing...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoogleUIBlue
                                    )
                                }
                            }
                        }
                    }

                    // Empty State if no apps matched
                    if (filteredList.isEmpty()) {
                        item(key = "empty_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.SearchOff,
                                        contentDescription = null,
                                        tint = googleColors.mediumGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No traffic found for this criteria",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = googleColors.mediumGray
                                    )
                                }
                            }
                        }
                    } else {
                        // Per-app cards
                        items(
                            items = filteredList,
                            key = { it.uid }
                        ) { app ->
                            AppItemCard(
                                app = app,
                                selectedFilter = uiState.selectedNetworkFilter,
                                maxBytesInList = maxBytesInList,
                                onClick = { viewModel.loadAppDetail(app) }
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // App Detail Bottom Sheet
    if (uiState.selectedAppDetail != null || uiState.isLoadingDetail) {
        val selectedApp = uiState.apps.firstOrNull { it.uid == uiState.selectedAppDetail?.uid }
        AppDetailBottomSheet(
            app = selectedApp,
            detail = uiState.selectedAppDetail,
            isLoading = uiState.isLoadingDetail,
            sheetState = sheetState,
            onDismiss = { viewModel.dismissAppDetail() }
        )
    }

    // Data Plan Dialog
    if (uiState.showDataPlanDialog) {
        DataPlanDialog(
            currentPlan = uiState.dataPlan,
            onSave = { limitBytes, warningPercent, isEnabled, planType ->
                viewModel.saveDataPlan(limitBytes, warningPercent, isEnabled, planType)
            },
            onDismiss = { viewModel.setShowDataPlanDialog(false) }
        )
    }

    // Architecture Info Dialog
    if (uiState.showArchInfoDialog) {
        ArchitectureInfoDialog(
            onDismiss = { viewModel.setShowArchInfoDialog(false) }
        )
    }
}
