package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderGray
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleUIBlue
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.LocalGoogleColors

@Composable
fun ArchitectureInfoDialog(
    onDismiss: () -> Unit
) {
    val googleColors = LocalGoogleColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("architecture_info_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = GoogleUIBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "NetworkStatsManager Architecture",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = googleColors.darkGray
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "How NetPulse queries persistent temporal buckets and resolves network usage on Android:",
                    style = MaterialTheme.typography.bodySmall,
                    color = googleColors.mediumGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pillar 1: API Evolution & Core Architecture
                PillarCard(
                    number = "1",
                    title = "API Evolution & Temporal Buckets",
                    color = GoogleBlue,
                    bgColor = googleColors.infoBg,
                    icon = Icons.Default.Layers,
                    content = "Introduced in Android 6.0, NetworkStatsManager replaced temporary Linux kernel files that wiped on reboot. It aggregates discrete Unix epoch time bins storing RX/TX bytes, UID, execution state, and network type."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pillar 2: Querying Methodologies
                PillarCard(
                    number = "2",
                    title = "Querying Methodologies",
                    color = GoogleGreen,
                    bgColor = googleColors.successBg,
                    icon = Icons.Default.QueryStats,
                    content = "• querySummaryForDevice(): Device-wide single aggregated bucket for Wi-Fi and Cellular totals.\n• querySummary(): Per-app loop via hasNextBucket() to accumulate bytes per numerical UID.\n• queryDetailsForUid(): Deep breakdown isolating STATE_FOREGROUND vs STATE_DEFAULT."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pillar 3: Android 10+ Privacy Constraints
                PillarCard(
                    number = "3",
                    title = "Android 10+ Privacy (Subscriber ID)",
                    color = GoogleYellow,
                    bgColor = googleColors.warningBg,
                    icon = Icons.Default.Lock,
                    content = "Telephony subscriberId required carrier privileges on Android 10+. NetPulse passes null as a wildcard to safely aggregate data across all active mobile subscriptions without throwing security exceptions."
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pillar 4: Android 11+ UID Mapping & Edge Cases
                PillarCard(
                    number = "4",
                    title = "UID Resolution & System Traffic",
                    color = GoogleRed,
                    bgColor = googleColors.errorBg,
                    icon = Icons.Default.Info,
                    content = "Data is logged against numerical UIDs. NetPulse resolves labels via PackageManager and catches system edge cases:\n• UID_REMOVED (-4): Uninstalled apps\n• UID_TETHERING (-5): Hotspot & Tethering\n• SYSTEM_UID (1000): Android OS & System"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time vs Historical Note
                PillarCard(
                    number = "5",
                    title = "Historical Buckets vs Real-Time Speed",
                    color = GoogleUIBlue,
                    bgColor = googleColors.infoBg,
                    icon = Icons.Default.Speed,
                    content = "NetworkStatsManager is strictly for historical aggregation. To display live speed (MB/s), NetPulse pairs it with ephemeral TrafficStats throughput counters computed every second."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoogleUIBlue),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun PillarCard(
    number: String,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    content: String
) {
    val googleColors = LocalGoogleColors.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderGray.copy(alpha = 0.5f))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = googleColors.darkGray
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = googleColors.darkGray
            )
        }
    }
}
