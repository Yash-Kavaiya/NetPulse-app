package com.example.ui.dashboard

import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.formatByteSize
import com.example.data.plan.CycleUsage

enum class TipTone { GOOD, INFO, WARNING }

data class UsageTip(val tone: TipTone, val title: String, val body: String)

/**
 * Rule-based, on-device observations about the user's usage. Nothing leaves the phone.
 */
object UsageTips {

    const val MAX_TIPS = 3
    private const val MIN_MEANINGFUL_BYTES = 50L * 1024 * 1024

    fun build(
        today: DeviceNetworkSummary,
        yesterday: DeviceNetworkSummary,
        cycleSummary: DeviceNetworkSummary,
        cycle: CycleUsage?,
        projectedBytes: Long?,
        topApps: List<AppNetworkUsage>
    ): List<UsageTip> {
        val tips = mutableListOf<UsageTip>()

        // 1. Plan pace.
        if (cycle != null && cycle.plan.isEnabled && cycle.plan.monthlyLimitBytes > 0 && projectedBytes != null) {
            val limit = cycle.plan.monthlyLimitBytes
            when {
                cycle.usedBytes >= limit -> tips += UsageTip(
                    TipTone.WARNING,
                    "You're over your plan",
                    "You've used ${formatByteSize(cycle.usedBytes)} of ${formatByteSize(limit)}. " +
                        "Turn on Data Saver and keep big downloads for Wi-Fi."
                )
                projectedBytes > limit -> tips += UsageTip(
                    TipTone.WARNING,
                    "On pace to exceed your plan",
                    "At this rate you'll reach about ${formatByteSize(projectedBytes)} before the cycle resets. " +
                        "Restricting background data for your top apps helps most."
                )
                cycle.usedBytes >= MIN_MEANINGFUL_BYTES -> tips += UsageTip(
                    TipTone.GOOD,
                    "Comfortably within your plan",
                    "You're on track to use about ${formatByteSize(projectedBytes)} of ${formatByteSize(limit)} this cycle."
                )
            }
        }

        // 2. A single app dominating the cycle.
        val cycleTotal = cycleSummary.grandTotalBytes
        val top = topApps.firstOrNull()
        if (top != null && cycleTotal >= MIN_MEANINGFUL_BYTES) {
            val share = (top.grandTotalBytes * 100 / cycleTotal).toInt()
            if (share >= 40) {
                tips += UsageTip(
                    TipTone.INFO,
                    "${top.appName} uses $share% of your data",
                    "That's ${formatByteSize(top.grandTotalBytes)} this cycle. Lowering its streaming or " +
                        "download quality would have the biggest effect."
                )
            }
        }

        // 3. Unusually heavy day.
        val todayBytes = today.grandTotalBytes
        val yesterdayBytes = yesterday.grandTotalBytes
        if (yesterdayBytes >= MIN_MEANINGFUL_BYTES && todayBytes > yesterdayBytes * 3 / 2) {
            tips += UsageTip(
                TipTone.INFO,
                "Heavier day than usual",
                "You've already used ${formatByteSize(todayBytes)} today, compared with " +
                    "${formatByteSize(yesterdayBytes)} for all of yesterday."
            )
        }

        // 4. Wi-Fi doing the heavy lifting.
        if (cycleTotal >= MIN_MEANINGFUL_BYTES) {
            val wifiShare = (cycleSummary.wifiTotalBytes * 100 / cycleTotal).toInt()
            if (wifiShare >= 80) {
                tips += UsageTip(
                    TipTone.GOOD,
                    "Wi-Fi carried $wifiShare% of your traffic",
                    "That kept ${formatByteSize(cycleSummary.wifiTotalBytes)} off your mobile plan this cycle."
                )
            } else if (wifiShare <= 30) {
                tips += UsageTip(
                    TipTone.INFO,
                    "Most of your data is mobile",
                    "Only $wifiShare% went over Wi-Fi. Connecting at home or work would stretch your plan."
                )
            }
        }

        return tips.take(MAX_TIPS)
    }
}
