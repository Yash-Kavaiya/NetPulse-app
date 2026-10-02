package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNetworkUsage
import com.example.data.model.NetworkFilterType
import com.example.data.model.formatByteSize
import com.example.ui.theme.BorderGray
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleUIBlue
import com.example.ui.theme.LocalGoogleColors

@Composable
fun AppItemCard(
    app: AppNetworkUsage,
    selectedFilter: NetworkFilterType,
    maxBytesInList: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val googleColors = LocalGoogleColors.current
    val bytes = app.getBytesForFilter(selectedFilter)
    val rxBytes = app.getRxBytesForFilter(selectedFilter)
    val txBytes = app.getTxBytesForFilter(selectedFilter)

    val progressFraction = if (maxBytesInList > 0) {
        (bytes.toFloat() / maxBytesInList).coerceIn(0.01f, 1f)
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_item_${app.uid}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderGray))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                AppIconView(
                    icon = app.icon,
                    uid = app.uid,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // App Name and Package/UID tag
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = googleColors.darkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (app.specialUidTag != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(googleColors.infoBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = app.specialUidTag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = GoogleUIBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (app.packageName != null) {
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = googleColors.mediumGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "UID ${app.uid}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = googleColors.mediumGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Usage Numbers & Chevron
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatByteSize(bytes),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = googleColors.darkGray
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // RX indicator
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "RX",
                            tint = GoogleBlue,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = formatByteSize(rxBytes),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = googleColors.mediumGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // TX indicator
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "TX",
                            tint = GoogleGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = formatByteSize(txBytes),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = googleColors.mediumGray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = googleColors.mediumGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Relative Proportional Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(googleColors.borderGray.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GoogleUIBlue)
                )
            }
        }
    }
}
