package com.example.ui.components

import android.content.Intent
import androidx.core.net.toUri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import com.example.data.model.UsageBucket
import com.example.ui.common.UsageBarChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppDetailBreakdown
import com.example.data.model.AppNetworkUsage
import com.example.data.model.formatByteSize
import com.example.ui.theme.BorderGray
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleUIBlue
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.LocalGoogleColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: AppNetworkUsage?,
    detail: AppDetailBreakdown?,
    isLoading: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    trend: List<UsageBucket> = emptyList()
) {
    if (app == null) return
    val context = LocalContext.current
    val googleColors = LocalGoogleColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("app_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with App Icon and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(
                    icon = app.icon,
                    uid = app.uid,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = googleColors.darkGray
                    )
                    Text(
                        text = app.packageName ?: (app.specialUidTag ?: "UID: ${app.uid}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = googleColors.mediumGray
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = googleColors.mediumGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Total Consumed Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(googleColors.infoBg)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Data Consumed",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoogleUIBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatByteSize(app.grandTotalBytes),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = googleColors.darkGray
                        )
                    }

                    // Download vs Upload Chips
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download",
                                tint = GoogleBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RX: ${formatByteSize(app.totalRxBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = googleColors.darkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Upload",
                                tint = GoogleGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TX: ${formatByteSize(app.totalTxBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = googleColors.darkGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Network Split (Wi-Fi vs Cellular)
            Text(
                text = "Network Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = googleColors.darkGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wi-Fi Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(googleColors.lightGray)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Wi-Fi",
                                tint = GoogleBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wi-Fi",
                                style = MaterialTheme.typography.labelMedium,
                                color = googleColors.darkGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatByteSize(app.totalBytesWifi),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = googleColors.darkGray
                        )
                    }
                }

                // Cellular Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(googleColors.lightGray)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NetworkCell,
                                contentDescription = "Cellular",
                                tint = GoogleGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cellular",
                                style = MaterialTheme.typography.labelMedium,
                                color = googleColors.darkGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatByteSize(app.totalBytesMobile),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = googleColors.darkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Execution State Breakdown (Foreground vs Background from queryDetailsForUid)
            Text(
                text = "Execution State (NetworkStats Bucket)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = googleColors.darkGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Isolated via queryDetailsForUid() looking at STATE_FOREGROUND vs STATE_DEFAULT.",
                style = MaterialTheme.typography.bodySmall,
                color = googleColors.mediumGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GoogleUIBlue,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (detail != null) {
                val fgTotal = detail.foregroundTotal
                val bgTotal = detail.backgroundTotal
                val totalExec = (fgTotal + bgTotal).coerceAtLeast(1L)
                val fgRatio = fgTotal.toFloat() / totalExec

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(googleColors.lightGray)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Foreground Usage",
                                style = MaterialTheme.typography.labelSmall,
                                color = googleColors.mediumGray
                            )
                            Text(
                                text = formatByteSize(fgTotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoogleGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Background Usage",
                                style = MaterialTheme.typography.labelSmall,
                                color = googleColors.mediumGray
                            )
                            Text(
                                text = formatByteSize(bgTotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoogleYellow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BorderGray)
                    ) {
                        if (fgRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(fgRatio.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(GoogleGreen)
                            )
                        }
                        if (1f - fgRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight((1f - fgRatio).coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(GoogleYellow)
                            )
                        }
                    }
                }
            }

            if (trend.any { it.totalBytes > 0 }) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Last 14 days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = googleColors.darkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                val dayFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
                UsageBarChart(
                    buckets = trend,
                    labelFor = { dayFormat.format(Date(it.startTime)) },
                    chartHeight = 120.dp,
                    modifier = Modifier.testTag("app_trend_chart")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Open Android App Info Settings if real package
            if (app.packageName != null && !app.isSpecialUid) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = "package:${app.packageName}".toUri()
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_system_settings_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleUIBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open App Settings in Android",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
