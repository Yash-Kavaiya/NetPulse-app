package com.example.ui

import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.plan.CycleUsage
import com.example.data.room.DataPlanEntity
import com.example.ui.dashboard.TipTone
import com.example.ui.dashboard.UsageTips
import com.example.ui.speedtest.SpeedRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageTipsTest {
    private val mb = 1024L * 1024
    private val gb = 1024 * mb
    private val plan = DataPlanEntity(monthlyLimitBytes = 10 * gb)

    private fun cycle(used: Long) = CycleUsage(plan, cycleStart = 0, cycleEnd = 30L * 86_400_000, usedBytes = used)

    @Test
    fun `no tips for a brand-new cycle with almost no data`() {
        val tiny = DeviceNetworkSummary(mobileRxBytes = 20 * 1024)
        val tips = UsageTips.build(tiny, DeviceNetworkSummary(), tiny, cycle(20 * 1024), projectedBytes = mb, topApps = emptyList())
        assertTrue(tips.isEmpty())
    }

    @Test
    fun `warns when projected usage exceeds the plan`() {
        val summary = DeviceNetworkSummary(mobileRxBytes = 6 * gb)
        val tips = UsageTips.build(summary, DeviceNetworkSummary(), summary, cycle(6 * gb), projectedBytes = 14 * gb, topApps = emptyList())
        assertEquals(TipTone.WARNING, tips.first().tone)
        assertEquals("On pace to exceed your plan", tips.first().title)
    }

    @Test
    fun `over-limit takes priority over the forecast warning`() {
        val summary = DeviceNetworkSummary(mobileRxBytes = 11 * gb)
        val tips = UsageTips.build(summary, DeviceNetworkSummary(), summary, cycle(11 * gb), projectedBytes = 20 * gb, topApps = emptyList())
        assertEquals("You're over your plan", tips.first().title)
    }

    @Test
    fun `calls out a dominant app and a wifi-heavy cycle`() {
        val summary = DeviceNetworkSummary(wifiRxBytes = 2 * gb, mobileRxBytes = 100 * mb)
        val apps = listOf(AppNetworkUsage(uid = 1, packageName = "com.video", appName = "Video", rxBytesWifi = (1.5 * gb).toLong()))
        val tips = UsageTips.build(summary, summary, summary, cycle = null, projectedBytes = null, topApps = apps)
        assertTrue(tips.any { it.title.startsWith("Video uses") })
        assertTrue(tips.any { it.title.startsWith("Wi-Fi carried") })
    }

    @Test
    fun `never returns more than the maximum number of tips`() {
        val today = DeviceNetworkSummary(mobileRxBytes = 9 * gb)
        val yesterday = DeviceNetworkSummary(mobileRxBytes = gb)
        val apps = listOf(AppNetworkUsage(uid = 1, packageName = "p", appName = "Hog", rxBytesMobile = 8 * gb))
        val tips = UsageTips.build(today, yesterday, today, cycle(9 * gb), projectedBytes = 30 * gb, topApps = apps)
        assertEquals(UsageTips.MAX_TIPS, tips.size)
    }

    @Test
    fun `speed rating reflects download speed and latency`() {
        assertTrue(SpeedRating.describe(150.0, 20).startsWith("Excellent"))
        assertTrue(SpeedRating.describe(150.0, 20).contains("Low latency"))
        assertTrue(SpeedRating.describe(1.0, 100).startsWith("Slow"))
        assertTrue(SpeedRating.describe(30.0, 300).contains("High latency"))
    }
}
