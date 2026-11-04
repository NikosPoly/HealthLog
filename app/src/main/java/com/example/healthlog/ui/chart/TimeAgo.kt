package com.example.healthlog.ui.chart

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

// How long ago something happened, e.g. 3 hours ago, yesterday, 5 days ago, 2 months ago.
// The screen turns it into text in the app's language.
data class TimeAgo(val amount: Long, val span: Span) {
    enum class Span { NOW, MINUTES, HOURS, YESTERDAY, DAYS, MONTHS, YEARS }
}

// Counts whole spans, by the calendar and the time of day, in the phone's time zone
// (the zone of now). For a measurement on 12 February at 12:00:
// - until 13 February 11:59: now (under a minute), minutes, then hours (up to 23 hours)
// - from 13 February 12:00: yesterday
// - from 14 February 12:00: 2 days, 3 days…, until 12 March 11:59 (27 days)
// - from 12 March 12:00: 1 month; from 12 April 12:00: 2 months…
// - from 12 February 12:00 next year: 1 year
// Months of 28, 30 or 31 days all count as one month. When the day doesn't exist in a
// month (31 January → February), the month is complete on the 1st of the month after.
// A time in the future (e.g. after a clock change) is "now".
fun timeAgo(timestamp: Long, now: ZonedDateTime): TimeAgo {
    val recorded = Instant.ofEpochMilli(timestamp).atZone(now.zone)
    val elapsed = Duration.between(recorded, now)
    val days = ChronoUnit.DAYS.between(recorded, now)
    val months = ChronoUnit.MONTHS.between(recorded, now)
    val years = ChronoUnit.YEARS.between(recorded, now)

    return when {
        elapsed < Duration.ofMinutes(1) -> TimeAgo(0, TimeAgo.Span.NOW)
        elapsed < Duration.ofHours(1) -> TimeAgo(elapsed.toMinutes(), TimeAgo.Span.MINUTES)
        days == 0L -> TimeAgo(elapsed.toHours(), TimeAgo.Span.HOURS)
        days == 1L -> TimeAgo(1, TimeAgo.Span.YESTERDAY)
        months == 0L -> TimeAgo(days, TimeAgo.Span.DAYS)
        years == 0L -> TimeAgo(months, TimeAgo.Span.MONTHS)
        else -> TimeAgo(years, TimeAgo.Span.YEARS)
    }
}
