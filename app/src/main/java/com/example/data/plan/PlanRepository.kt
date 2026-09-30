package com.example.data.plan

import com.example.data.model.DeviceNetworkSummary
import com.example.data.room.DataPlanDao
import com.example.data.room.DataPlanEntity
import com.example.data.stats.TimeRanges
import com.example.data.stats.UsageStatsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Usage of the current billing cycle against the configured plan. */
data class CycleUsage(
    val plan: DataPlanEntity,
    val cycleStart: Long,
    val cycleEnd: Long,
    val usedBytes: Long
) {
    val fraction: Float
        get() = if (plan.monthlyLimitBytes > 0) usedBytes.toFloat() / plan.monthlyLimitBytes else 0f
    val remainingBytes: Long get() = (plan.monthlyLimitBytes - usedBytes).coerceAtLeast(0L)
}

enum class PlanAlert { NONE, WARNING, LIMIT }

object PlanAlerts {
    /** Bytes that count against the plan for the given summary. */
    fun countedBytes(plan: DataPlanEntity, summary: DeviceNetworkSummary): Long =
        if (plan.planType == DataPlanEntity.PLAN_TYPE_TOTAL) summary.grandTotalBytes else summary.mobileTotalBytes

    /**
     * Decides which alert (if any) is due. Each alert fires at most once per billing cycle:
     * the plan remembers the cycle start for which it last alerted.
     */
    fun evaluate(plan: DataPlanEntity, usedBytes: Long, cycleStart: Long): PlanAlert {
        if (!plan.isEnabled || plan.monthlyLimitBytes <= 0) return PlanAlert.NONE
        val limitReached = usedBytes >= plan.monthlyLimitBytes
        val warningReached = usedBytes * 100 >= plan.monthlyLimitBytes * plan.warningPercent
        return when {
            limitReached && plan.limitAlertedCycle != cycleStart -> PlanAlert.LIMIT
            !limitReached && warningReached && plan.warningAlertedCycle != cycleStart -> PlanAlert.WARNING
            else -> PlanAlert.NONE
        }
    }

    fun markAlerted(plan: DataPlanEntity, alert: PlanAlert, cycleStart: Long): DataPlanEntity = when (alert) {
        // Reaching the limit implies the warning is also done for this cycle.
        PlanAlert.LIMIT -> plan.copy(limitAlertedCycle = cycleStart, warningAlertedCycle = cycleStart)
        PlanAlert.WARNING -> plan.copy(warningAlertedCycle = cycleStart)
        PlanAlert.NONE -> plan
    }
}

@Singleton
class PlanRepository @Inject constructor(
    private val dao: DataPlanDao,
    private val stats: UsageStatsSource
) {
    val plan: Flow<DataPlanEntity> = dao.getDataPlan().map { it ?: DataPlanEntity() }

    suspend fun current(): DataPlanEntity = dao.getDataPlanOnce() ?: DataPlanEntity()

    suspend fun save(plan: DataPlanEntity) = dao.savePlan(plan)

    suspend fun cycleUsage(now: Long = System.currentTimeMillis()): CycleUsage {
        val plan = current()
        val start = TimeRanges.billingCycleStart(plan.cycleStartDay, now)
        val end = TimeRanges.nextBillingCycleStart(plan.cycleStartDay, now)
        val summary = stats.deviceSummary(start, now)
        return CycleUsage(plan, start, end, PlanAlerts.countedBytes(plan, summary))
    }
}
