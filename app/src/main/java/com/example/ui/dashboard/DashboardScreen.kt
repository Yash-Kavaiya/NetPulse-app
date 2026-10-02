package com.example.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.NetworkFilterType
import com.example.data.model.TimeRangeFilter
import com.example.data.model.UsageBucket
import com.example.data.model.formatByteSize
import com.example.data.plan.CycleUsage
import com.example.data.room.DataPlanEntity
import com.example.data.stats.TimeRanges
import com.example.ui.common.ErrorBanner
import com.example.ui.common.LoadingState
import com.example.ui.common.OnResume
import com.example.ui.common.PermissionGate
import com.example.ui.common.SectionCard
import com.example.ui.components.AppIconView
import com.example.ui.components.DataPlanDialog
import com.example.ui.components.DataSummaryCard
import com.example.ui.theme.LocalGoogleColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenApps: () -> Unit,
    onOpenSpeedTest: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val liveSpeed by viewModel.liveSpeed.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()

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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                state.errorMessage?.let { item(key = "error") { ErrorBanner(it, onRetry = viewModel::refresh) } }

                item(key = "hero") {
                    HeroCard(
                        today = state.today,
                        yesterday = state.yesterday,
                        week = state.week,
                        liveSpeed = liveSpeed,
                        onClick = onOpenHistory
                    )
                }

                item(key = "plan") {
                    PlanCard(
                        plan = plan,
                        cycle = state.cycle,
                        projectedBytes = state.projectedCycleBytes,
                        onEdit = { viewModel.showPlanDialog(true) }
                    )
                }

                val tips = state.tips
                if (tips.isNotEmpty()) {
                    item(key = "tips") { TipsCard(tips) }
                }

                if (state.topApps.isNotEmpty()) {
                    item(key = "top_apps") {
                        TopAppsCard(apps = state.topApps, onOpenApps = onOpenApps)
                    }
                }

                item(key = "cycle") {
                    DataSummaryCard(
                        summary = state.cycleSummary,
                        selectedFilter = NetworkFilterType.ALL,
                        timeRange = TimeRangeFilter.BILLING_CYCLE,
                        dataPlan = null,
                        liveSpeed = liveSpeed,
                        onEditDataPlan = {}
                    )
                }

                item(key = "speed_cta") { SpeedTestCta(onOpenSpeedTest) }
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

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

/** Counts up to [target] bytes the first time it appears and whenever it changes. */
@Composable
private fun animatedBytes(target: Long): Long {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val value by animateFloatAsState(
        targetValue = if (started) target.toFloat() else 0f,
        animationSpec = tween(900),
        label = "bytes"
    )
    return value.toLong()
}

@Composable
private fun HeroCard(
    today: DeviceNetworkSummary,
    yesterday: DeviceNetworkSummary,
    week: List<UsageBucket>,
    liveSpeed: LiveTrafficSpeed,
    onClick: () -> Unit
) {
    val colors = LocalGoogleColors.current
    val onHero = Color.White
    val onHeroMuted = Color.White.copy(alpha = 0.78f)
    val todayBytes = today.grandTotalBytes
    val yesterdayBytes = yesterday.grandTotalBytes

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
            .clickable(onClickLabel = "Open history", onClick = onClick)
            .padding(20.dp)
            .testTag("hero_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                greeting(),
                style = MaterialTheme.typography.labelLarge,
                color = onHeroMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF7CF29C)))
                Spacer(Modifier.width(6.dp))
                Text(
                    "↓ ${liveSpeed.rxFormatted}  ↑ ${liveSpeed.txFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onHero,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatByteSize(animatedBytes(todayBytes)),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = onHero,
                modifier = Modifier
                    .testTag("today_total")
                    .semantics { contentDescription = "${formatByteSize(todayBytes)} used today" }
            )
            Text(
                "today",
                style = MaterialTheme.typography.titleMedium,
                color = onHeroMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }

        if (yesterdayBytes > 0) {
            val diff = todayBytes - yesterdayBytes
            val pct = (abs(diff) * 100 / yesterdayBytes).toInt()
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (diff > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = onHero,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "$pct% ${if (diff > 0) "more" else "less"} than yesterday (${formatByteSize(yesterdayBytes)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = onHeroMuted
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroStat("Mobile", formatByteSize(today.mobileTotalBytes), Modifier.weight(1f))
            HeroStat("Wi-Fi", formatByteSize(today.wifiTotalBytes), Modifier.weight(1f))
        }

        if (week.any { it.totalBytes > 0 }) {
            Spacer(Modifier.height(16.dp))
            WeekSparkline(week)
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/** Seven slim bars, one per day, with today highlighted. */
@Composable
private fun WeekSparkline(week: List<UsageBucket>) {
    val max = week.maxOf { it.totalBytes }.coerceAtLeast(1L)
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "Usage for the last ${week.size} days" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEachIndexed { i, day ->
            val isToday = i == week.lastIndex
            val fraction by animateFloatAsState(
                targetValue = (day.totalBytes.toFloat() / max).coerceIn(0.06f, 1f),
                animationSpec = tween(700),
                label = "bar"
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(44.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = if (isToday) 0.95f else 0.38f))
                    )
                }
                Text(
                    dayFormat.format(Date(day.startTime)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White.copy(alpha = if (isToday) 1f else 0.7f),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: DataPlanEntity, cycle: CycleUsage?, projectedBytes: Long?, onEdit: () -> Unit) {
    val colors = LocalGoogleColors.current
    if (!plan.isEnabled || cycle == null) {
        SectionCard {
            Text(
                "Set a data plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.darkGray
            )
            Text(
                "Track your billing cycle, get a forecast and receive alerts before you run out.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mediumGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(onClick = onEdit, modifier = Modifier.testTag("set_plan_button")) { Text("Set up plan") }
        }
        return
    }

    val fraction = cycle.fraction
    val ringColor = when {
        fraction >= 1f -> colors.red
        fraction * 100 >= plan.warningPercent -> colors.yellow
        else -> colors.green
    }
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    val daysLeft = ceil((cycle.cycleEnd - System.currentTimeMillis()).toDouble() / TimeRanges.DAY_MS).toInt().coerceAtLeast(0)
    val kind = if (plan.planType == DataPlanEntity.PLAN_TYPE_MOBILE) "mobile data" else "data"

    SectionCard(modifier = Modifier.testTag("plan_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (plan.planType == DataPlanEntity.PLAN_TYPE_MOBILE) "Mobile data plan" else "Data plan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.darkGray,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).testTag("edit_plan_button")) {
                Icon(Icons.Default.Edit, contentDescription = "Edit data plan", tint = colors.mediumGray, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(96.dp).semantics {
                    contentDescription = "${(fraction * 100).toInt()} percent of plan used"
                },
                contentAlignment = Alignment.Center
            ) {
                val track = colors.lightGray
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val d = size.minDimension - stroke
                    val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
                    drawArc(track, -90f, 360f, false, topLeft, Size(d, d), style = Stroke(stroke))
                    drawArc(
                        ringColor, -90f, 360f * animated, false, topLeft, Size(d, d),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.darkGray
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${formatByteSize(cycle.usedBytes)} of ${formatByteSize(plan.monthlyLimitBytes)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.darkGray
                )
                Text(
                    "${formatByteSize(cycle.remainingBytes)} left · resets in $daysLeft ${if (daysLeft == 1) "day" else "days"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mediumGray
                )
                if (projectedBytes != null) {
                    val over = projectedBytes > plan.monthlyLimitBytes
                    Text(
                        "Forecast: about ${formatByteSize(projectedBytes)} of $kind this cycle",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (over) colors.red else colors.green,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TipsCard(tips: List<UsageTip>) {
    val colors = LocalGoogleColors.current
    SectionCard(title = "Smart tips", modifier = Modifier.testTag("tips_card")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tips.forEach { tip ->
                val (bg, tint, icon) = when (tip.tone) {
                    TipTone.GOOD -> Triple(colors.successBg, colors.green, Icons.Default.CheckCircle)
                    TipTone.INFO -> Triple(colors.infoBg, colors.uiBlue, Icons.Default.Lightbulb)
                    TipTone.WARNING -> Triple(colors.warningBg, colors.yellow, Icons.Default.Warning)
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(12.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            tip.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.darkGray
                        )
                        Text(tip.body, style = MaterialTheme.typography.bodySmall, color = colors.mediumGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAppsCard(apps: List<AppNetworkUsage>, onOpenApps: () -> Unit) {
    val colors = LocalGoogleColors.current
    val max = apps.maxOf { it.grandTotalBytes }.coerceAtLeast(1L)
    SectionCard {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenApps).padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Top apps this cycle",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.darkGray,
                modifier = Modifier.weight(1f)
            )
            Text("See all", style = MaterialTheme.typography.labelLarge, color = colors.uiBlue)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.uiBlue)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            apps.forEach { app ->
                val fraction by animateFloatAsState(
                    (app.grandTotalBytes.toFloat() / max).coerceIn(0.02f, 1f), tween(700), label = "share"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconView(icon = app.icon, uid = app.uid, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(
                                app.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.darkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formatByteSize(app.grandTotalBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.darkGray
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.lightGray)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Brush.horizontalGradient(listOf(colors.uiBlue, colors.blue)))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedTestCta(onClick: () -> Unit) {
    val colors = LocalGoogleColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.infoBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("speed_cta"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Speed, contentDescription = null, tint = colors.uiBlue, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "How fast is your connection?",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.darkGray
            )
            Text("Run a quick speed test", style = MaterialTheme.typography.bodySmall, color = colors.mediumGray)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.uiBlue)
    }
}
