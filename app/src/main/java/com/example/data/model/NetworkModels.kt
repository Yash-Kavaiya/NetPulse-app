package com.example.data.model

import android.graphics.drawable.Drawable
import java.util.Locale

enum class NetworkFilterType(val label: String) {
    ALL("All"),
    MOBILE("Mobile"),
    WIFI("Wi-Fi")
}

enum class TimeRangeFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    BILLING_CYCLE("Billing Cycle")
}

enum class SortOption(val label: String) {
    TOTAL_DESC("Most Data Used"),
    RX_DESC("Highest Download"),
    TX_DESC("Highest Upload"),
    NAME_ASC("App Name (A-Z)")
}

data class AppNetworkUsage(
    val uid: Int,
    val packageName: String?,
    val appName: String,
    val icon: Drawable? = null,
    val rxBytesMobile: Long = 0L,
    val txBytesMobile: Long = 0L,
    val rxBytesWifi: Long = 0L,
    val txBytesWifi: Long = 0L,
    val isSpecialUid: Boolean = false,
    val specialUidTag: String? = null
) {
    val totalBytesMobile: Long get() = rxBytesMobile + txBytesMobile
    val totalBytesWifi: Long get() = rxBytesWifi + txBytesWifi
    val totalRxBytes: Long get() = rxBytesMobile + rxBytesWifi
    val totalTxBytes: Long get() = txBytesMobile + txBytesWifi
    val grandTotalBytes: Long get() = totalBytesMobile + totalBytesWifi

    fun getBytesForFilter(filter: NetworkFilterType): Long = when (filter) {
        NetworkFilterType.ALL -> grandTotalBytes
        NetworkFilterType.MOBILE -> totalBytesMobile
        NetworkFilterType.WIFI -> totalBytesWifi
    }

    fun getRxBytesForFilter(filter: NetworkFilterType): Long = when (filter) {
        NetworkFilterType.ALL -> totalRxBytes
        NetworkFilterType.MOBILE -> rxBytesMobile
        NetworkFilterType.WIFI -> rxBytesWifi
    }

    fun getTxBytesForFilter(filter: NetworkFilterType): Long = when (filter) {
        NetworkFilterType.ALL -> totalTxBytes
        NetworkFilterType.MOBILE -> txBytesMobile
        NetworkFilterType.WIFI -> txBytesWifi
    }
}

data class DeviceNetworkSummary(
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val startTime: Long = 0L,
    val endTime: Long = 0L
) {
    val mobileTotalBytes: Long get() = mobileRxBytes + mobileTxBytes
    val wifiTotalBytes: Long get() = wifiRxBytes + wifiTxBytes
    val totalRxBytes: Long get() = mobileRxBytes + wifiRxBytes
    val totalTxBytes: Long get() = mobileTxBytes + wifiTxBytes
    val grandTotalBytes: Long get() = mobileTotalBytes + wifiTotalBytes

    fun getTotalForFilter(filter: NetworkFilterType): Long = when (filter) {
        NetworkFilterType.ALL -> grandTotalBytes
        NetworkFilterType.MOBILE -> mobileTotalBytes
        NetworkFilterType.WIFI -> wifiTotalBytes
    }

    fun getRxForFilter(filter: NetworkFilterType): Long = when (filter) {
        NetworkFilterType.ALL -> totalRxBytes
        NetworkFilterType.MOBILE -> mobileRxBytes
        NetworkFilterType.WIFI -> wifiRxBytes
    }

    fun getTxForFilter(filter: NetworkFilterType): Long = when (filter) {
        NetworkFilterType.ALL -> totalTxBytes
        NetworkFilterType.MOBILE -> mobileTxBytes
        NetworkFilterType.WIFI -> wifiTxBytes
    }
}

data class AppDetailBreakdown(
    val uid: Int,
    val appName: String,
    val packageName: String?,
    val foregroundRxBytes: Long = 0L,
    val foregroundTxBytes: Long = 0L,
    val backgroundRxBytes: Long = 0L,
    val backgroundTxBytes: Long = 0L,
    val mobileBytes: Long = 0L,
    val wifiBytes: Long = 0L,
    val totalBytes: Long = 0L
) {
    val foregroundTotal: Long get() = foregroundRxBytes + foregroundTxBytes
    val backgroundTotal: Long get() = backgroundRxBytes + backgroundTxBytes
    val downloadTotal: Long get() = foregroundRxBytes + backgroundRxBytes
    val uploadTotal: Long get() = foregroundTxBytes + backgroundTxBytes
}

data class LiveTrafficSpeed(
    val rxSpeedBps: Long = 0L,
    val txSpeedBps: Long = 0L,
    val isAvailable: Boolean = true
) {
    val rxFormatted: String get() = formatSpeed(rxSpeedBps)
    val txFormatted: String get() = formatSpeed(txSpeedBps)

    companion object {
        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0) return "0 B/s"
            val kb = bytesPerSec / 1024.0
            if (kb < 1024.0) {
                return String.format(Locale.US, "%.1f KB/s", kb)
            }
            val mb = kb / 1024.0
            return String.format(Locale.US, "%.2f MB/s", mb)
        }
    }
}

fun formatByteSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

/** Mbps formatting for speed-test results (network convention: bits, base 10). */
fun formatMbps(mbps: Double): String = String.format(Locale.US, "%.1f Mbps", mbps)

/** One bucket of device-wide traffic, used for history charts. */
data class UsageBucket(
    val startTime: Long,
    val endTime: Long,
    val mobileBytes: Long = 0L,
    val wifiBytes: Long = 0L
) {
    val totalBytes: Long get() = mobileBytes + wifiBytes
}
