package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.NetworkFilterType
import com.example.data.model.TimeRangeFilter
import com.example.data.model.formatByteSize
import com.example.data.room.DataPlanEntity
import com.example.ui.theme.BorderGray
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleUIBlue
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.LocalGoogleColors

@Composable
fun DataSummaryCard(
    summary: DeviceNetworkSummary,
    selectedFilter: NetworkFilterType,
    timeRange: TimeRangeFilter,
    dataPlan: DataPlanEntity?,
    liveSpeed: LiveTrafficSpeed,
    onEditDataPlan: () -> Unit,
    modifier: Modifier = Modifier,
    /** Bytes used in the current billing cycle; the plan tracker is hidden when null. */
    planUsedBytes: Long? = null
) {
    val googleColors = LocalGoogleColors.current
    val totalBytes = summary.getTotalForFilter(selectedFilter)
    val rxBytes = summary.getRxForFilter(selectedFilter)
    val txBytes = summary.getTxForFilter(selectedFilter)

    val mobileTotal = summary.mobileTotalBytes
    val wifiTotal = summary.wifiTotalBytes
    val combined = (mobileTotal + wifiTotal).coerceAtLeast(1L)
    val mobileRatio = (mobileTotal.toFloat() / combined).coerceIn(0f, 1f)
    val wifiRatio = (wifiTotal.toFloat() / combined).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("data_summary_card")
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderGray))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Time filter indicator and Live Speed Ticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeRange.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = googleColors.mediumGray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                // Live Speed Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(googleColors.infoBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GoogleGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "↓ ${liveSpeed.rxFormatted}  ↑ ${liveSpeed.txFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoogleUIBlue,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Large Byte Metric
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatByteSize(totalBytes),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = googleColors.darkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (selectedFilter) {
                        NetworkFilterType.ALL -> "combined"
                        NetworkFilterType.MOBILE -> "cellular"
                        NetworkFilterType.WIFI -> "Wi-Fi"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = googleColors.mediumGray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual Proportional Ratio Bar (Cellular vs Wi-Fi)
            if (combined > 1L) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GoogleBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wi-Fi (${(wifiRatio * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = googleColors.darkGray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GoogleGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cellular (${(mobileRatio * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = googleColors.darkGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Segmented Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(googleColors.borderGray)
                    ) {
                        if (wifiRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(wifiRatio.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(GoogleBlue)
                            )
                        }
                        if (mobileRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(mobileRatio.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(GoogleGreen)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-metrics Grid (Download RX vs Upload TX)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(googleColors.lightGray)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Download Metric
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(googleColors.infoBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download",
                            tint = GoogleUIBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Download (RX)",
                            style = MaterialTheme.typography.labelSmall,
                            color = googleColors.mediumGray
                        )
                        Text(
                            text = formatByteSize(rxBytes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = googleColors.darkGray
                        )
                    }
                }

                // Upload Metric
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(googleColors.successBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload",
                            tint = GoogleGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Upload (TX)",
                            style = MaterialTheme.typography.labelSmall,
                            color = googleColors.mediumGray
                        )
                        Text(
                            text = formatByteSize(txBytes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = googleColors.darkGray
                        )
                    }
                }
            }

            // Data Plan Budget Tracker (if enabled)
            if (dataPlan != null && dataPlan.isEnabled && planUsedBytes != null) {
                val budgetBytes = dataPlan.monthlyLimitBytes.coerceAtLeast(1L)
                val relevantUsed = planUsedBytes
                val usedProgress = (relevantUsed.toFloat() / budgetBytes).coerceIn(0f, 1.5f)
                val animatedProgress by animateFloatAsState(targetValue = usedProgress.coerceAtMost(1f), label = "progress")
                val isWarning = usedProgress >= (dataPlan.warningPercent / 100f)
                val isExceeded = usedProgress >= 1f

                val progressColor = when {
                    isExceeded -> GoogleRed
                    isWarning -> GoogleYellow
                    else -> GoogleGreen
                }

                val planBgColor = when {
                    isExceeded -> googleColors.errorBg
                    isWarning -> googleColors.warningBg
                    else -> googleColors.infoBg
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(planBgColor)
                        .clickable(onClick = onEditDataPlan)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isWarning || isExceeded) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Warning",
                                        tint = progressColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (dataPlan.planType == DataPlanEntity.PLAN_TYPE_MOBILE) "Cellular Data Budget" else "Total Data Budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = googleColors.darkGray
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formatByteSize(relevantUsed)} / ${formatByteSize(budgetBytes)} (${(usedProgress * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isExceeded) GoogleRed else googleColors.mediumGray,
                                    fontWeight = if (isExceeded) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Budget",
                                    tint = googleColors.mediumGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(googleColors.borderGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .background(progressColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
