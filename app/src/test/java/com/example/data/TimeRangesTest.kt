package com.example.data

import com.example.data.model.TimeRangeFilter
import com.example.data.stats.TimeRanges
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimeRangesTest {
    private val tz = TimeZone.getTimeZone("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun `cycle start in current month when day has passed`() {
        assertEquals(at(2026, 9, 5, 0), TimeRanges.billingCycleStart(5, at(2026, 9, 20), tz))
    }

    @Test
    fun `cycle start falls back to previous month before reset day`() {
        assertEquals(at(2026, 8, 25, 0), TimeRanges.billingCycleStart(25, at(2026, 9, 10), tz))
    }

    @Test
    fun `cycle day 31 clamps to last day of short months`() {
        // 15 March: the 31st of March is still ahead, so the cycle began on Feb 28 (2026 is not a leap year).
        assertEquals(at(2026, 2, 28, 0), TimeRanges.billingCycleStart(31, at(2026, 3, 15), tz))
        assertEquals(at(2026, 3, 31, 0), TimeRanges.nextBillingCycleStart(31, at(2026, 3, 15), tz))
    }

    @Test
    fun `cycle resets exactly at midnight of the reset day`() {
        val midnight = at(2026, 9, 1, 0)
        assertEquals(midnight, TimeRanges.billingCycleStart(1, midnight, tz))
    }

    @Test
    fun `next cycle crosses year boundary`() {
        assertEquals(at(2027, 1, 10, 0), TimeRanges.nextBillingCycleStart(10, at(2026, 12, 20), tz))
    }

    @Test
    fun `yesterday spans exactly one day`() {
        val now = at(2026, 9, 30, 15)
        val (start, end) = TimeRanges.bounds(TimeRangeFilter.YESTERDAY, now = now, timeZone = tz)
        assertEquals(at(2026, 9, 29, 0), start)
        assertEquals(at(2026, 9, 30, 0), end)
    }

    @Test
    fun `last 7 days includes today`() {
        val now = at(2026, 9, 30, 15)
        val (start, end) = TimeRanges.bounds(TimeRangeFilter.LAST_7_DAYS, now = now, timeZone = tz)
        assertEquals(at(2026, 9, 24, 0), start)
        assertEquals(now, end)
        assertEquals(7, TimeRanges.dayStarts(start, end, tz).size)
    }
}
