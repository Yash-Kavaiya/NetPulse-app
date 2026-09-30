package com.example.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NetworkFilterType
import com.example.data.model.TimeRangeFilter
import com.example.data.model.formatByteSize
import com.example.ui.common.ErrorBanner
import com.example.ui.common.LoadingState
import com.example.ui.common.OnResume
import com.example.ui.common.PermissionGate
import com.example.ui.common.SectionCard
import com.example.ui.components.AppIconView
import com.example.ui.components.DataPlanDialog
import com.example.ui.components.DataSummaryCard
import com.example.ui.theme.LocalGoogleColors
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenApps: () -> Unit,
    onOpenSpeedTest: () -> Unit,
    onOpenInsights: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val liveSpeed by viewModel.liveSpeed.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val colors = LocalGoogleColors.current

    OnResume { viewModel.checkPermissionAndLoad() }

    PermissionGate(state.hasPermission, onRecheck = viewModel::checkPermissionAndLoad) {
        if (state.isLoading) {
            LoadingState("Loading your usage…")
            return@PermissionGate
        }
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("dashboard_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.errorMessage?.let { item { ErrorBanner(it, onRetry = viewModel::refresh) } }

                item(key = "today") {
                    SectionCard(title = "Today") {
                        val todayBytes = state.today.grandTotalBytes
                        val yesterdayBytes = state.yesterday.grandTotalBytes
                        Text(
                            formatByteSize(todayBytes),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.darkGray,
                            modifier = Modifier.testTag("today_total")
                        )
                        Text(
                            "Mobile ${formatByteSize(state.today.mobileTotalBytes)} · Wi-Fi ${formatByteSize(state.today.wifiTotalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.mediumGray
                        )
                        if (yesterdayBytes > 0) {
                            val diff = todayBytes - yesterdayBytes
                            val pct = (abs(diff) * 100 / yesterdayBytes).toInt()
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (diff > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (diff > 0) colors.red else colors.green
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "$pct% ${if (diff > 0) "more" else "less"} than yesterday so far (${formatByteSize(yesterdayBytes)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mediumGray
                                )
                            }
                        }
                    }
                }

                item(key = "cycle") {
                    DataSummaryCard(
                        summary = state.cycleSummary,
                        selectedFilter = NetworkFilterType.ALL,
                        timeRange = TimeRangeFilter.BILLING_CYCLE,
                        dataPlan = plan,
                        liveSpeed = liveSpeed,
                        onEditDataPlan = { viewModel.showPlanDialog(true) },
                        planUsedBytes = state.cycle?.usedBytes
                    )
                }

                state.cycle?.takeIf { plan.isEnabled }?.let { cycle ->
                    item(key = "forecast") {
                        SectionCard(title = "Cycle forecast") {
                            val projected = state.projectedCycleBytes ?: 0L
                            val over = projected > plan.monthlyLimitBytes
                            Text(
                                "At your current pace you'll use about ${formatByteSize(projected)} by " +
                                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(cycle.cycleEnd)) + ".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.darkGray
                            )
                            Text(
                                if (over) "That's over your ${formatByteSize(plan.monthlyLimitBytes)} plan."
                                else "${formatByteSize(cycle.remainingBytes)} left this cycle.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (over) colors.red else colors.green,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                item(key = "actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = onOpenSpeedTest,
                            label = { Text("Run speed test") },
                            leadingIcon = { Icon(Icons.Default.Speed, null) }
                        )
                        AssistChip(
                            onClick = onOpenInsights,
                            label = { Text("AI insights") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null) }
                        )
                    }
                }

                if (state.topApps.isNotEmpty()) {
                    item(key = "top_apps") {
                        SectionCard(title = "Top apps this cycle") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.topApps.forEach { app ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable(onClick = onOpenApps),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppIconView(icon = app.icon, uid = app.uid, size = 32.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            app.appName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.darkGray,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        Text(
                                            formatByteSize(app.grandTotalBytes),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.darkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showPlanDialog) {
        DataPlanDialog(
            currentPlan = plan,
            onSave = viewModel::savePlan,
            onDismiss = { viewModel.showPlanDialog(false) }
        )
    }
}
