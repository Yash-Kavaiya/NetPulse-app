package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.R
import com.example.data.model.formatByteSize
import com.example.data.plan.CycleUsage
import com.example.data.plan.PlanRepository
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun stats(): UsageStatsSource
    fun planRepository(): PlanRepository
}

private data class WidgetData(val hasPermission: Boolean, val todayBytes: Long, val cycle: CycleUsage?)

/** Home-screen widget showing today's usage and billing-cycle progress. */
class UsageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val data = runCatching {
            val stats = ep.stats()
            if (!stats.hasPermission()) {
                WidgetData(false, 0L, null)
            } else {
                val now = System.currentTimeMillis()
                val today = stats.deviceSummary(TimeRanges.startOfDay(now), now).grandTotalBytes
                WidgetData(true, today, ep.planRepository().cycleUsage(now))
            }
        }.getOrElse { WidgetData(false, 0L, null) }

        provideContent { GlanceTheme { Content(context, data) } }
    }

    @Composable
    private fun Content(context: Context, data: WidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(4.dp))
            if (!data.hasPermission) {
                Text(
                    text = context.getString(R.string.widget_grant_access),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp)
                )
                return@Column
            }
            Text(
                text = formatByteSize(data.todayBytes),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = context.getString(R.string.widget_today),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
            )
            val cycle = data.cycle
            if (cycle != null && cycle.plan.isEnabled) {
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = cycle.fraction.coerceIn(0f, 1f),
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = if (cycle.fraction >= 1f) GlanceTheme.colors.error else GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.surfaceVariant
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(
                        R.string.widget_cycle,
                        formatByteSize(cycle.usedBytes),
                        formatByteSize(cycle.plan.monthlyLimitBytes)
                    ),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                )
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            runCatching { UsageWidget().updateAll(context) }
        }
    }
}

class UsageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()
}
