package com.example.data.stats

import com.example.data.model.TimeRangeFilter
import java.util.Calendar
import java.util.TimeZone

/**
 * Pure time-window math, kept free of Android APIs so it can be unit tested.
 */
object TimeRanges {

    const val DAY_MS = 24L * 60L * 60L * 1000L

    fun bounds(
        filter: TimeRangeFilter,
        cycleStartDay: Int = 1,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Long, Long> {
        val startOfToday = startOfDay(now, timeZone)
        return when (filter) {
            TimeRangeFilter.TODAY -> startOfToday to now
            TimeRangeFilter.YESTERDAY -> addDays(startOfToday, -1, timeZone) to startOfToday
            TimeRangeFilter.LAST_7_DAYS -> addDays(startOfToday, -6, timeZone) to now
            TimeRangeFilter.LAST_30_DAYS -> addDays(startOfToday, -29, timeZone) to now
            TimeRangeFilter.BILLING_CYCLE -> billingCycleStart(cycleStartDay, now, timeZone) to now
        }
    }

    /**
     * Start of the billing cycle containing [now]. A cycle day greater than the number of days
     * in a month (e.g. 31 in February) resets on that month's last day.
     */
    fun billingCycleStart(
        cycleStartDay: Int,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val day = cycleStartDay.coerceIn(1, 31)
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = startOfDay(now, timeZone) }
        val candidate = cycleDayIn(cal, day)
        if (candidate.timeInMillis <= now) return candidate.timeInMillis
        cal.add(Calendar.MONTH, -1)
        return cycleDayIn(cal, day).timeInMillis
    }

    /** Start of the next cycle after the one containing [now]. */
    fun nextBillingCycleStart(
        cycleStartDay: Int,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = billingCycleStart(cycleStartDay, now, timeZone)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
        }
        return cycleDayIn(cal, cycleStartDay.coerceIn(1, 31)).timeInMillis
    }

    fun startOfDay(time: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun addDays(time: Long, days: Int, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = time
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis

    /** Day boundaries (start-of-day timestamps) covering [start, end). */
    fun dayStarts(start: Long, end: Long, timeZone: TimeZone = TimeZone.getDefault()): List<Long> {
        val result = mutableListOf<Long>()
        var cursor = startOfDay(start, timeZone)
        while (cursor < end) {
            result += cursor
            cursor = addDays(cursor, 1, timeZone)
        }
        return result
    }

    private fun cycleDayIn(monthCal: Calendar, day: Int): Calendar =
        (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}
