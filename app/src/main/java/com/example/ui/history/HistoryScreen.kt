package com.example.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.formatByteSize
import com.example.ui.common.LoadingState
import com.example.ui.common.OnResume
import com.example.ui.common.PermissionGate
import com.example.ui.common.SectionCard
import com.example.ui.common.UsageBarChart
import com.example.ui.theme.LocalGoogleColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalGoogleColors.current
    val dayFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val weekdayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val fullFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    OnResume { viewModel.checkPermissionAndLoad() }

    PermissionGate(state.hasPermission, onRecheck = viewModel::checkPermissionAndLoad) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("history_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "range") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    HistoryRange.entries.forEachIndexed { i, range ->
                        SegmentedButton(
                            selected = state.range == range,
                            onClick = { viewModel.setRange(range) },
                            shape = SegmentedButtonDefaults.itemShape(i, HistoryRange.entries.size)
                        ) { Text(range.label) }
                    }
                }
            }

            if (state.isLoading) {
                item { Box(Modifier.height(240.dp)) { LoadingState() } }
                return@LazyColumn
            }

            item(key = "chart") {
                SectionCard(title = "Daily usage") {
                    UsageBarChart(
                        buckets = state.days,
                        labelFor = {
                            if (state.range == HistoryRange.WEEK) weekdayFormat.format(Date(it.startTime))
                            else dayFormat.format(Date(it.startTime))
                        },
                        selectedIndex = state.selectedDayIndex,
                        onBarSelected = viewModel::selectDay,
                        modifier = Modifier.testTag("daily_chart")
                    )
                    Text(
                        "Tap a bar to see hourly usage",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mediumGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            state.selectedDayIndex?.let { idx ->
                val day = state.days.getOrNull(idx)
                if (day != null) {
                    item(key = "hourly") {
                        SectionCard(title = "${fullFormat.format(Date(day.startTime))} · ${formatByteSize(day.totalBytes)}") {
                            if (state.isLoadingHourly) {
                                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                UsageBarChart(
                                    buckets = state.hourly,
                                    labelFor = { SimpleDateFormat("HH", Locale.getDefault()).format(Date(it.startTime)) },
                                    chartHeight = 120.dp
                                )
                            }
                        }
                    }
                }
            }

            item(key = "stats") {
                SectionCard(title = "Summary") {
                    StatRow("Total", formatByteSize(state.total))
                    StatRow("Mobile", formatByteSize(state.mobileTotal))
                    StatRow("Wi-Fi", formatByteSize(state.wifiTotal))
                    StatRow("Daily average", formatByteSize(state.dailyAverage))
                    state.peakDay?.takeIf { it.totalBytes > 0 }?.let {
                        StatRow("Busiest day", "${dayFormat.format(Date(it.startTime))} · ${formatByteSize(it.totalBytes)}")
                    }
                    state.changePercent?.let { pct ->
                        StatRow(
                            "vs previous ${state.range.label}",
                            (if (pct >= 0) "+" else "") + "$pct%",
                            valueColor = if (pct > 0) colors.red else colors.green
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    val colors = LocalGoogleColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.mediumGray)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (valueColor == Color.Unspecified) colors.darkGray else valueColor
            )
        }
    }
}
