package com.example.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.plan.PlanAlert
import com.example.data.plan.PlanAlerts
import com.example.data.plan.PlanRepository
import com.example.data.prefs.SettingsRepository
import com.example.data.room.DailyUsageDao
import com.example.data.room.DailyUsageEntity
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import com.example.notifications.Notifications
import com.example.widget.UsageWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Persists device-wide daily totals so history survives the platform's NetworkStats retention
 * window, then refreshes the home-screen widget.
 */
@HiltWorker
class UsageSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stats: UsageStatsSource,
    private val dailyUsageDao: DailyUsageDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!stats.hasPermission()) return Result.success()
        return try {
            val now = System.currentTimeMillis()
            val today = TimeRanges.startOfDay(now)
            // Re-snapshot the last few days so late-arriving buckets are captured.
            val days = TimeRanges.dayStarts(TimeRanges.addDays(today, -BACKFILL_DAYS), now)
            val rows = days.mapIndexed { i, start ->
                val end = days.getOrNull(i + 1) ?: now
                val s = stats.deviceSummary(start, end)
                DailyUsageEntity(start, s.mobileRxBytes, s.mobileTxBytes, s.wifiRxBytes, s.wifiTxBytes)
            }
            dailyUsageDao.upsert(rows)
            dailyUsageDao.deleteOlderThan(TimeRanges.addDays(today, -RETENTION_DAYS))
            UsageWidget.updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val NAME = "usage_snapshot"
        const val BACKFILL_DAYS = 3
        const val RETENTION_DAYS = 400
    }
}

/** Checks billing-cycle usage against the data plan and posts at most one alert per threshold. */
@HiltWorker
class DataLimitCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stats: UsageStatsSource,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!stats.hasPermission()) return Result.success()
        if (!settingsRepository.current().alertsEnabled) return Result.success()
        return try {
            val usage = planRepository.cycleUsage()
            val alert = PlanAlerts.evaluate(usage.plan, usage.usedBytes, usage.cycleStart)
            if (alert != PlanAlert.NONE) {
                Notifications.showPlanAlert(applicationContext, alert, usage)
                planRepository.save(PlanAlerts.markAlerted(usage.plan, alert, usage.cycleStart))
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val NAME = "data_limit_check"
    }
}

object WorkScheduler {
    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            UsageSnapshotWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UsageSnapshotWorker>(6, TimeUnit.HOURS).build()
        )
        wm.enqueueUniquePeriodicWork(
            DataLimitCheckWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DataLimitCheckWorker>(1, TimeUnit.HOURS).build()
        )
    }

    /** Runs both workers immediately, e.g. after the plan changes. */
    fun runNow(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork("${UsageSnapshotWorker.NAME}_now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<UsageSnapshotWorker>().build())
        wm.enqueueUniqueWork("${DataLimitCheckWorker.NAME}_now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<DataLimitCheckWorker>().build())
    }
}
