package com.example.data.stats

import android.content.Context
import com.example.data.model.AppDetailBreakdown
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.UsageBucket
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over the platform NetworkStatsManager so view models and workers can be tested
 * with fakes.
 */
interface UsageStatsSource {
    fun hasPermission(): Boolean
    suspend fun deviceSummary(start: Long, end: Long): DeviceNetworkSummary
    suspend fun apps(start: Long, end: Long): List<AppNetworkUsage>
    suspend fun appDetail(app: AppNetworkUsage, start: Long, end: Long): AppDetailBreakdown
    suspend fun deviceBuckets(boundaries: List<Long>, end: Long): List<UsageBucket>
    suspend fun appDailyBuckets(uid: Int, dayStarts: List<Long>, end: Long): List<UsageBucket>

    /** Emits live throughput once per [intervalMs] while collected. */
    fun liveSpeed(intervalMs: Long = 1000L): Flow<LiveTrafficSpeed>
}

@Singleton
class StatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UsageStatsSource {

    private val io: CoroutineDispatcher = Dispatchers.IO

    override fun hasPermission(): Boolean = NetworkStatsHelper.hasUsageAccessPermission(context)

    override suspend fun deviceSummary(start: Long, end: Long): DeviceNetworkSummary =
        withContext(io) { NetworkStatsHelper.queryDeviceSummary(context, start, end) }

    override suspend fun apps(start: Long, end: Long): List<AppNetworkUsage> =
        withContext(io) { NetworkStatsHelper.queryAppsSummary(context, start, end) }

    override suspend fun appDetail(app: AppNetworkUsage, start: Long, end: Long): AppDetailBreakdown =
        withContext(io) {
            NetworkStatsHelper.queryUidDetails(context, app.uid, app.appName, app.packageName, start, end)
        }

    override suspend fun deviceBuckets(boundaries: List<Long>, end: Long): List<UsageBucket> =
        withContext(io) { NetworkStatsHelper.queryDeviceBuckets(context, boundaries, end) }

    override suspend fun appDailyBuckets(uid: Int, dayStarts: List<Long>, end: Long): List<UsageBucket> =
        withContext(io) { NetworkStatsHelper.queryUidDailyBuckets(context, uid, dayStarts, end) }

    override fun liveSpeed(intervalMs: Long): Flow<LiveTrafficSpeed> = flow {
        val tracker = NetworkStatsHelper.RealtimeSpeedTracker()
        while (true) {
            delay(intervalMs)
            emit(tracker.sampleSpeed())
        }
    }.flowOn(io)
}
