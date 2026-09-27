package com.example

import com.example.data.model.AppNetworkUsage
import com.example.data.model.DeviceNetworkSummary
import com.example.data.model.LiveTrafficSpeed
import com.example.data.model.NetworkFilterType
import com.example.data.model.TimeRangeFilter
import com.example.data.stats.NetworkStatsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAppNetworkUsageCalculations() {
    val usage = AppNetworkUsage(
      uid = 10042,
      packageName = "com.test.app",
      appName = "Test App",
      rxBytesMobile = 1000L,
      txBytesMobile = 200L,
      rxBytesWifi = 5000L,
      txBytesWifi = 1000L
    )

    assertEquals(1200L, usage.totalBytesMobile)
    assertEquals(6000L, usage.totalBytesWifi)
    assertEquals(6000L, usage.totalRxBytes)
    assertEquals(1200L, usage.totalTxBytes)
    assertEquals(7200L, usage.grandTotalBytes)

    assertEquals(7200L, usage.getBytesForFilter(NetworkFilterType.ALL))
    assertEquals(1200L, usage.getBytesForFilter(NetworkFilterType.MOBILE))
    assertEquals(6000L, usage.getBytesForFilter(NetworkFilterType.WIFI))
  }

  @Test
  fun testDeviceSummaryCalculations() {
    val summary = DeviceNetworkSummary(
      mobileRxBytes = 2000L,
      mobileTxBytes = 500L,
      wifiRxBytes = 10000L,
      wifiTxBytes = 2000L
    )

    assertEquals(2500L, summary.mobileTotalBytes)
    assertEquals(12000L, summary.wifiTotalBytes)
    assertEquals(14500L, summary.grandTotalBytes)
  }

  @Test
  fun testSpeedFormatting() {
    assertEquals("0 B/s", LiveTrafficSpeed.formatSpeed(0L))
    assertEquals("100.0 KB/s", LiveTrafficSpeed.formatSpeed(100L * 1024L))
    assertEquals("2.50 MB/s", LiveTrafficSpeed.formatSpeed((2.5 * 1024 * 1024).toLong()))
  }

  @Test
  fun testTimeBounds() {
    val (start, end) = NetworkStatsHelper.getTimeBounds(TimeRangeFilter.TODAY)
    assertTrue(start > 0)
    assertTrue(end >= start)
  }
}

