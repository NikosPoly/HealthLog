package com.example.healthlog.ui.chart

import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.runtime.Composable
import com.example.healthlog.ui.format.formatLocale
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

// How long ago something happened, e.g. 3 hours ago, yesterday, 5 days ago, 2 months ago.
// timeAgo works it out; timeAgoText turns it into text in the app's language.
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

// "2 days ago" / «πριν από 2 ημέρες», "yesterday" / «χθες», in the app's language,
// written by Android's built-in formatter (ICU).
@Composable
internal fun timeAgoText(timeAgo: TimeAgo): String {
    val formatter = RelativeDateTimeFormatter.getInstance(formatLocale())
    val unit = when (timeAgo.span) {
        TimeAgo.Span.NOW -> return formatter.format(
            RelativeDateTimeFormatter.Direction.PLAIN,
            RelativeDateTimeFormatter.AbsoluteUnit.NOW
        )
        TimeAgo.Span.YESTERDAY -> return formatter.format(
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.AbsoluteUnit.DAY
        )
        TimeAgo.Span.MINUTES -> RelativeDateTimeFormatter.RelativeUnit.MINUTES
        TimeAgo.Span.HOURS -> RelativeDateTimeFormatter.RelativeUnit.HOURS
        TimeAgo.Span.DAYS -> RelativeDateTimeFormatter.RelativeUnit.DAYS
        TimeAgo.Span.MONTHS -> RelativeDateTimeFormatter.RelativeUnit.MONTHS
        TimeAgo.Span.YEARS -> RelativeDateTimeFormatter.RelativeUnit.YEARS
    }
    return formatter.format(
        timeAgo.amount.toDouble(),
        RelativeDateTimeFormatter.Direction.LAST,
        unit
    )
}
