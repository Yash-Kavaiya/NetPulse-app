package com.example.data

import com.example.data.model.DeviceNetworkSummary
import com.example.data.plan.PlanAlert
import com.example.data.plan.PlanAlerts
import com.example.data.room.DataPlanEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanAlertsTest {
    private val gb = 1024L * 1024 * 1024
    private val plan = DataPlanEntity(monthlyLimitBytes = 10 * gb, warningPercent = 80)
    private val cycle = 1_000L

    @Test
    fun `no alert below warning threshold`() {
        assertEquals(PlanAlert.NONE, PlanAlerts.evaluate(plan, 7 * gb, cycle))
    }

    @Test
    fun `warning at threshold, then not repeated in the same cycle`() {
        assertEquals(PlanAlert.WARNING, PlanAlerts.evaluate(plan, 8 * gb, cycle))
        val alerted = PlanAlerts.markAlerted(plan, PlanAlert.WARNING, cycle)
        assertEquals(PlanAlert.NONE, PlanAlerts.evaluate(alerted, 9 * gb, cycle))
    }

    @Test
    fun `limit fires after warning and marks both`() {
        val warned = PlanAlerts.markAlerted(plan, PlanAlert.WARNING, cycle)
        assertEquals(PlanAlert.LIMIT, PlanAlerts.evaluate(warned, 10 * gb, cycle))
        val limited = PlanAlerts.markAlerted(warned, PlanAlert.LIMIT, cycle)
        assertEquals(PlanAlert.NONE, PlanAlerts.evaluate(limited, 12 * gb, cycle))
    }

    @Test
    fun `alerts re-arm in a new cycle`() {
        val limited = PlanAlerts.markAlerted(plan, PlanAlert.LIMIT, cycle)
        assertEquals(PlanAlert.WARNING, PlanAlerts.evaluate(limited, 8 * gb, cycle + 1))
    }

    @Test
    fun `disabled plan never alerts`() {
        assertEquals(PlanAlert.NONE, PlanAlerts.evaluate(plan.copy(isEnabled = false), 20 * gb, cycle))
    }

    @Test
    fun `plan type selects counted bytes`() {
        val summary = DeviceNetworkSummary(mobileRxBytes = 3, mobileTxBytes = 2, wifiRxBytes = 10, wifiTxBytes = 5)
        assertEquals(5L, PlanAlerts.countedBytes(plan, summary))
        assertEquals(20L, PlanAlerts.countedBytes(plan.copy(planType = DataPlanEntity.PLAN_TYPE_TOTAL), summary))
    }
}
