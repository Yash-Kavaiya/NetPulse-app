package com.example.ui.speedtest

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.formatMbps
import com.example.data.room.SpeedTestResultEntity
import com.example.data.speedtest.SpeedTestPhase
import com.example.ui.common.ErrorBanner
import com.example.ui.common.SectionCard
import com.example.ui.theme.LocalGoogleColors
import java.text.DateFormat
import java.util.Date
import kotlin.math.ln
import kotlin.math.min

@Composable
fun SpeedTestScreen(viewModel: SpeedTestViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val colors = LocalGoogleColors.current
    val p = state.progress

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "gauge") {
            SectionCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val shownMbps = when (p.phase) {
                        SpeedTestPhase.DOWNLOAD, SpeedTestPhase.UPLOAD -> p.currentMbps
                        SpeedTestPhase.DONE -> p.downloadMbps ?: 0.0
                        else -> 0.0
                    }
                    SpeedGauge(
                        mbps = shownMbps,
                        arcColor = if (p.phase == SpeedTestPhase.UPLOAD) colors.green else colors.uiBlue
                    )
                    Text(
                        text = when (p.phase) {
                            SpeedTestPhase.IDLE -> "Ready"
                            SpeedTestPhase.PING -> "Measuring latency…"
                            SpeedTestPhase.DOWNLOAD -> "Testing download…"
                            SpeedTestPhase.UPLOAD -> "Testing upload…"
                            SpeedTestPhase.DONE -> "Done · ${state.networkType}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.mediumGray,
                        modifier = Modifier.padding(top = 8.dp).testTag("speed_phase")
                    )
                    if (state.isRunning) {
                        val overall = when (p.phase) {
                            SpeedTestPhase.PING -> p.phaseProgress * 0.1f
                            SpeedTestPhase.DOWNLOAD -> 0.1f + p.phaseProgress * 0.45f
                            SpeedTestPhase.UPLOAD -> 0.55f + p.phaseProgress * 0.45f
                            else -> 0f
                        }
                        val animatedOverall by animateFloatAsState(overall, label = "overall")
                        LinearProgressIndicator(
                            progress = { animatedOverall },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            trackColor = colors.lightGray,
                            drawStopIndicator = {}
                        )
                    }
                    if (p.phase == SpeedTestPhase.DONE) {
                        Text(
                            SpeedRating.describe(p.downloadMbps ?: 0.0, p.pingMs ?: 0L),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.darkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.successBg)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("speed_rating")
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Metric(
                            "Download",
                            (p.downloadMbps ?: p.currentMbps.takeIf { p.phase == SpeedTestPhase.DOWNLOAD })?.let(::formatMbps) ?: "—"
                        )
                        Metric(
                            "Upload",
                            (p.uploadMbps ?: p.currentMbps.takeIf { p.phase == SpeedTestPhase.UPLOAD })?.let(::formatMbps) ?: "—"
                        )
                        Metric("Ping", p.pingMs?.let { "$it ms" } ?: "—")
                        Metric("Jitter", p.jitterMs?.let { "$it ms" } ?: "—")
                    }
                    if (state.isRunning) {
                        OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Cancel")
                        }
                    } else {
                        Button(
                            onClick = viewModel::start,
                            modifier = Modifier.padding(top = 16.dp).testTag("start_speed_test")
                        ) { Text(if (p.phase == SpeedTestPhase.DONE) "Test again" else "Start test") }
                    }
                    Text(
                        "Uses about 50–300 MB depending on your speed. Powered by Cloudflare's speed test endpoints.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mediumGray,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        state.error?.let { msg ->
            item(key = "error") { ErrorBanner(msg, onRetry = { viewModel.dismissError(); viewModel.start() }) }
        }

        if (history.isNotEmpty()) {
            item(key = "history_header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Previous results",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.darkGray,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
                }
            }
            items(history, key = { it.id }) { HistoryRow(it) }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    val colors = LocalGoogleColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.mediumGray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.darkGray)
    }
}

@Composable
private fun HistoryRow(result: SpeedTestResultEntity) {
    val colors = LocalGoogleColors.current
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(format.format(Date(result.timestamp)), style = MaterialTheme.typography.bodySmall, color = colors.mediumGray)
                Text(result.networkType, style = MaterialTheme.typography.labelSmall, color = colors.mediumGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "↓ ${formatMbps(result.downloadMbps)}  ↑ ${formatMbps(result.uploadMbps)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.darkGray
                )
                Text("${result.pingMs} ms ping", style = MaterialTheme.typography.labelSmall, color = colors.mediumGray)
            }
        }
    }
}

/** Plain-language verdict for a finished test. */
object SpeedRating {
    fun describe(downloadMbps: Double, pingMs: Long): String {
        val speed = when {
            downloadMbps >= 100 -> "Excellent — 4K streaming on several devices and large downloads."
            downloadMbps >= 25 -> "Fast — smooth 4K streaming and video calls."
            downloadMbps >= 10 -> "Good — HD streaming and video calls work well."
            downloadMbps >= 3 -> "Okay — fine for browsing, music and SD video."
            else -> "Slow — expect buffering on video."
        }
        val latency = when {
            pingMs <= 0 -> ""
            pingMs <= 50 -> " Low latency, great for gaming."
            pingMs <= 150 -> ""
            else -> " High latency may cause lag in calls and games."
        }
        return speed + latency
    }
}

/** 270° arc gauge on a log scale up to 1 Gbps. */
@Composable
private fun SpeedGauge(mbps: Double, arcColor: Color) {
    val colors = LocalGoogleColors.current
    val target = (ln(1 + mbps) / ln(1 + 1000.0)).toFloat().coerceIn(0f, 1f)
    val sweep by animateFloatAsState(target, label = "gauge")
    val track = colors.lightGray
    Box(
        Modifier.size(220.dp).semantics { contentDescription = "Current speed ${formatMbps(mbps)}" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val d = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 135f, 270f, false, topLeft, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            if (sweep > 0.005f) {
                drawArc(arcColor, 135f, 270f * sweep, false, topLeft, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                String.format(java.util.Locale.US, "%.1f", mbps),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = colors.darkGray
            )
            Text("Mbps", style = MaterialTheme.typography.labelLarge, color = colors.mediumGray)
        }
    }
}
