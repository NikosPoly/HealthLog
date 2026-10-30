package com.example.healthlog.ui.chart

import com.example.healthlog.database.Measurement
import java.time.ZonedDateTime

// How far back the chart and its stats look. Only the chart uses it; the history list
// always shows everything.
// Each period counts back from now, keeping the time of day: 7 days means from this time
// seven days ago. A date that doesn't exist becomes the month's last day
// (6 months before 31 August is 28 February).
enum class ChartPeriod {
    SEVEN_DAYS,
    THIRTY_DAYS,
    SIX_MONTHS,
    ONE_YEAR,
    ALL;

    // The earliest time included, in milliseconds, or null when nothing is left out.
    fun startTime(now: ZonedDateTime): Long? {
        val start = when (this) {
            SEVEN_DAYS -> now.minusDays(7)
            THIRTY_DAYS -> now.minusDays(30)
            SIX_MONTHS -> now.minusMonths(6)
            ONE_YEAR -> now.minusYears(1)
            ALL -> return null
        }
        return start.toInstant().toEpochMilli()
    }
}

// The measurements recorded within the period, in their original order.
fun List<Measurement>.inPeriod(period: ChartPeriod, now: ZonedDateTime): List<Measurement> {
    val start = period.startTime(now) ?: return this
    return filter { it.timestamp >= start }
}
