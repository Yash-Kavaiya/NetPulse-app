package com.example.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.UsageBucket
import com.example.data.model.formatByteSize
import com.example.ui.theme.LocalGoogleColors

/**
 * Stacked bar chart (mobile on the bottom, Wi-Fi on top) drawn directly on a Canvas.
 * Tapping a bar reports its index via [onBarSelected].
 */
@Composable
fun UsageBarChart(
    buckets: List<UsageBucket>,
    labelFor: (UsageBucket) -> String,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onBarSelected: (Int) -> Unit = {},
    chartHeight: Dp = 180.dp,
    mobileColor: Color = LocalGoogleColors.current.uiBlue,
    wifiColor: Color = LocalGoogleColors.current.green
) {
    val colors = LocalGoogleColors.current
    val maxBytes = (buckets.maxOfOrNull { it.totalBytes } ?: 0L).coerceAtLeast(1L)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(600))
    }
    val total = buckets.sumOf { it.totalBytes }
    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .semantics {
                    contentDescription = "Usage chart with ${buckets.size} bars, total ${formatByteSize(total)}"
                }
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        if (buckets.isEmpty()) return@detectTapGestures
                        val slot = size.width / buckets.size
                        onBarSelected((offset.x / slot).toInt().coerceIn(0, buckets.lastIndex))
                    }
                }
        ) {
            if (buckets.isEmpty()) return@Canvas
            val slot = size.width / buckets.size
            val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            val radius = CornerRadius(barWidth / 4, barWidth / 4)
            // Guide lines at 50% and 100% of the max bar.
            listOf(0f, 0.5f).forEach { f ->
                val y = size.height * f
                drawLine(
                    color = colors.borderGray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }
            buckets.forEachIndexed { i, bucket ->
                val left = slot * i + (slot - barWidth) / 2
                val mobileH = size.height * (bucket.mobileBytes.toFloat() / maxBytes) * progress.value
                val wifiH = size.height * (bucket.wifiBytes.toFloat() / maxBytes) * progress.value
                val dim = selectedIndex != null && selectedIndex != i
                val alpha = if (dim) 0.35f else 1f
                if (wifiH > 0f) {
                    drawRoundRect(
                        color = wifiColor.copy(alpha = alpha),
                        topLeft = Offset(left, size.height - mobileH - wifiH),
                        size = Size(barWidth, wifiH),
                        cornerRadius = radius
                    )
                }
                if (mobileH > 0f) {
                    drawRoundRect(
                        color = mobileColor.copy(alpha = alpha),
                        topLeft = Offset(left, size.height - mobileH),
                        size = Size(barWidth, mobileH),
                        cornerRadius = radius
                    )
                }
            }
        }
        // Sparse x-axis labels so they never overlap.
        if (buckets.isNotEmpty()) {
            val step = ((buckets.size + 6) / 7).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                buckets.forEachIndexed { i, b ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (i % step == 0) {
                            Text(
                                labelFor(b),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mediumGray,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Legend(mobileColor, "Mobile")
            Legend(wifiColor, "Wi-Fi")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalGoogleColors.current.mediumGray)
    }
}
