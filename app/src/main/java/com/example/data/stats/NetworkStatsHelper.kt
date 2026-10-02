package com.example.data.stats

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import com.example.data.model.AppDetailBreakdown
import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.TimeRangeFilter
import java.util.Calendar

object NetworkStatsHelper {

    // Special UID constants recognized by Android NetworkStats
    const val UID_REMOVED = -4 // NetworkStats.Bucket.UID_REMOVED
    const val UID_TETHERING = -5 // NetworkStats.Bucket.UID_TETHERING
    const val SYSTEM_UID = Process.SYSTEM_UID // 1000

    /**
     * Checks if the app has been granted PACKAGE_USAGE_STATS permission
     * via Settings > Apps > Special app access > Usage access.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculates time bounds for standard temporal queries
     */
    fun getTimeBounds(filter: TimeRangeFilter): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        return when (filter) {
            TimeRangeFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            TimeRangeFilter.YESTERDAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val endOfYesterday = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val startOfYesterday = calendar.timeInMillis
                Pair(startOfYesterday, endOfYesterday)
            }
            TimeRangeFilter.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                Pair(calendar.timeInMillis, now)
            }
            TimeRangeFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
        }
    }

    /**
     * 1. Query device-wide summary using NetworkStatsManager.querySummaryForDevice()
     * Subscriber ID is explicitly null as per Android 10+ privacy constraint.
     */
    fun queryDeviceSummary(context: Context, startTime: Long, endTime: Long): DeviceNetworkSummary {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return DeviceNetworkSummary(startTime = startTime, endTime = endTime)

        var mobileRx = 0L
        var mobileTx = 0L
        var wifiRx = 0L
        var wifiTx = 0L

        // Query Mobile Summary (subscriberId is null for Android 10+ wildcard aggregation)
        try {
            val mobileBucket = nsm.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            mobileRx = mobileBucket.rxBytes
            mobileTx = mobileBucket.txBytes
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }

        // Query Wi-Fi Summary
        try {
            val wifiBucket = nsm.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            wifiRx = wifiBucket.rxBytes
            wifiTx = wifiBucket.txBytes
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }

        return DeviceNetworkSummary(
            mobileRxBytes = mobileRx,
            mobileTxBytes = mobileTx,
            wifiRxBytes = wifiRx,
            wifiTxBytes = wifiTx,
            startTime = startTime,
            endTime = endTime
        )
    }

    private data class UidAggregator(
        var rxMobile: Long = 0L,
        var txMobile: Long = 0L,
        var rxWifi: Long = 0L,
        var txWifi: Long = 0L
    )

    /**
     * 2. Query per-application network usage breakdown using NetworkStatsManager.querySummary()
     * Iterates temporal buckets via hasNextBucket() and accumulates bytes per UID.
     * Maps UIDs to human-readable names and handles special system edge cases (UID_REMOVED, UID_TETHERING, SYSTEM_UID).
     */
    fun queryAppsSummary(context: Context, startTime: Long, endTime: Long): List<AppNetworkUsage> {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return emptyList()

        val uidMap = mutableMapOf<Int, UidAggregator>()

        // 1. Accumulate Mobile Stats
        try {
            val mobileStats = nsm.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            val bucket = NetworkStats.Bucket()
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                val agg = uidMap.getOrPut(bucket.uid) { UidAggregator() }
                agg.rxMobile += bucket.rxBytes
                agg.txMobile += bucket.txBytes
            }
            mobileStats.close()
        } catch (_: Exception) {
        }

        // 2. Accumulate Wi-Fi Stats
        try {
            val wifiStats = nsm.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                val agg = uidMap.getOrPut(bucket.uid) { UidAggregator() }
                agg.rxWifi += bucket.rxBytes
                agg.txWifi += bucket.txBytes
            }
            wifiStats.close()
        } catch (_: Exception) {
        }

        // 3. Resolve UIDs via PackageManager & Edge-case classifiers
        val pm = context.packageManager
        val result = mutableListOf<AppNetworkUsage>()

        for ((uid, agg) in uidMap) {
            val total = agg.rxMobile + agg.txMobile + agg.rxWifi + agg.txWifi
            if (total <= 0) continue // Skip UIDs with 0 traffic in this window

            val resolved = resolveUid(pm, uid)
            result.add(
                AppNetworkUsage(
                    uid = uid,
                    packageName = resolved.packageName,
                    appName = resolved.label,
                    icon = resolved.icon,
                    rxBytesMobile = agg.rxMobile,
                    txBytesMobile = agg.txMobile,
                    rxBytesWifi = agg.rxWifi,
                    txBytesWifi = agg.txWifi,
                    isSpecialUid = resolved.isSpecial,
                    specialUidTag = resolved.specialTag
                )
            )
        }

        return result.sortedByDescending { it.grandTotalBytes }
    }

    /**
     * 3. Query Detailed foreground vs background usage for a specific UID
     * using NetworkStatsManager.queryDetailsForUid()
     */
    fun queryUidDetails(
        context: Context,
        uid: Int,
        appName: String,
        packageName: String?,
        startTime: Long,
        endTime: Long
    ): AppDetailBreakdown {
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return AppDetailBreakdown(uid = uid, appName = appName, packageName = packageName)

        var fgRx = 0L
        var fgTx = 0L
        var bgRx = 0L
        var bgTx = 0L
        var mobileTotal = 0L
        var wifiTotal = 0L

        // Inspect Mobile buckets for UID
        try {
            val mobileDetails = nsm.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime,
                uid
            )
            val bucket = NetworkStats.Bucket()
            while (mobileDetails.hasNextBucket()) {
                mobileDetails.getNextBucket(bucket)
                val bytes = bucket.rxBytes + bucket.txBytes
                mobileTotal += bytes
                if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                    fgRx += bucket.rxBytes
                    fgTx += bucket.txBytes
                } else {
                    bgRx += bucket.rxBytes
                    bgTx += bucket.txBytes
                }
            }
            mobileDetails.close()
        } catch (_: Exception) {
        }

        // Inspect Wi-Fi buckets for UID
        try {
            val wifiDetails = nsm.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime,
                uid
            )
            val bucket = NetworkStats.Bucket()
            while (wifiDetails.hasNextBucket()) {
                wifiDetails.getNextBucket(bucket)
                val bytes = bucket.rxBytes + bucket.txBytes
                wifiTotal += bytes
                if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                    fgRx += bucket.rxBytes
                    fgTx += bucket.txBytes
                } else {
                    bgRx += bucket.rxBytes
                    bgTx += bucket.txBytes
                }
            }
            wifiDetails.close()
        } catch (_: Exception) {
        }

        return AppDetailBreakdown(
            uid = uid,
            appName = appName,
            packageName = packageName,
            foregroundRxBytes = fgRx,
            foregroundTxBytes = fgTx,
            backgroundRxBytes = bgRx,
            backgroundTxBytes = bgTx,
            mobileBytes = mobileTotal,
            wifiBytes = wifiTotal,
            totalBytes = mobileTotal + wifiTotal
        )
    }

    private data class ResolvedApp(
        val label: String,
        val packageName: String?,
        val icon: android.graphics.drawable.Drawable? = null,
        val isSpecial: Boolean = false,
        val specialTag: String? = null
    )

    /**
     * Resolves UID into human readable label and icon.
     * Specifically handles:
     * - UID_REMOVED (-4): Uninstalled / Removed Apps
     * - UID_TETHERING (-5): Hotspot & Tethering
     * - SYSTEM_UID (1000): Android OS & System
     * - Root (0): Kernel / Root
     * - Package names mapped via PackageManager.getPackagesForUid()
     */
    private fun resolveUid(pm: PackageManager, uid: Int): ResolvedApp {
        when (uid) {
            UID_REMOVED -> return ResolvedApp(
                label = "Removed / Uninstalled Apps",
                packageName = null,
                isSpecial = true,
                specialTag = "UID_REMOVED (-4)"
            )
            UID_TETHERING -> return ResolvedApp(
                label = "Hotspot & Tethering",
                packageName = null,
                isSpecial = true,
                specialTag = "UID_TETHERING (-5)"
            )
            SYSTEM_UID -> return ResolvedApp(
                label = "Android OS & System",
                packageName = "android",
                isSpecial = true,
                specialTag = "SYSTEM_UID (1000)"
            )
            0 -> return ResolvedApp(
                label = "Linux Kernel / Root",
                packageName = null,
                isSpecial = true,
                specialTag = "ROOT (0)"
            )
            1013 -> return ResolvedApp(
                label = "Media Server (Audio/Video)",
                packageName = null,
                isSpecial = true,
                specialTag = "MEDIA_UID (1013)"
            )
            1021 -> return ResolvedApp(
                label = "GPS & Location Daemon",
                packageName = null,
                isSpecial = true,
                specialTag = "GPS_UID (1021)"
            )
        }

        // Standard UID: Try resolving package name
        try {
            val packages = pm.getPackagesForUid(uid)
            if (!packages.isNullOrEmpty()) {
                val primaryPkg = packages[0]
                val appInfo: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(primaryPkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(primaryPkg, 0)
                }

                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val extraTag = if (packages.size > 1) "Shared UID (${packages.size} apps)" else null

                return ResolvedApp(
                    label = label,
                    packageName = primaryPkg,
                    icon = icon,
                    isSpecial = false,
                    specialTag = extraTag
                )
            }
        } catch (_: Exception) {
        }

        // Fallback for unresolved system or daemon UIDs
        return ResolvedApp(
            label = "System Service (UID $uid)",
            packageName = null,
            isSpecial = true,
            specialTag = "UID $uid"
        )
    }

    /**
     * Real-time network speed monitor using ephemeral TrafficStats counters.
     * Computes the throughput rate (Bytes per second) between periodic ticks.
     */
    class RealtimeSpeedTracker {
        private var lastRxBytes = TrafficStats.getTotalRxBytes()
        private var lastTxBytes = TrafficStats.getTotalTxBytes()
        private var lastTimestamp = System.currentTimeMillis()

        fun sampleSpeed(): LiveTrafficSpeed {
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            val currentTimestamp = System.currentTimeMillis()

            val deltaMs = (currentTimestamp - lastTimestamp).coerceAtLeast(100L)
            val deltaSeconds = deltaMs / 1000.0

            val rxDelta = (currentRx - lastRxBytes).coerceAtLeast(0L)
            val txDelta = (currentTx - lastTxBytes).coerceAtLeast(0L)

            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastTimestamp = currentTimestamp

            val rxSpeed = (rxDelta / deltaSeconds).toLong()
            val txSpeed = (txDelta / deltaSeconds).toLong()

            return LiveTrafficSpeed(
                rxSpeedBps = rxSpeed,
                txSpeedBps = txSpeed,
                isAvailable = currentRx != TrafficStats.UNSUPPORTED.toLong()
            )
        }
    }
}
