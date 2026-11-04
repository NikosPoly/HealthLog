package com.example.healthlog.ui.chart

import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatLocale
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import java.time.ZonedDateTime

// The stats that don't depend on the period, below the period stats (or below the
// "needs measurements" message): when the last measurement was recorded, e.g. "2 days ago",
// and that measurement with its date and time.
@Composable
fun LatestStatsSection(
    // The most recent measurement of the type.
    latest: Measurement,
    modifier: Modifier = Modifier
) {
    val separator = decimalSeparator()
    val dateTimeFormatter = rememberDateTimeFormatter()
    val lastRecorded = timeAgoText(timeAgo(latest.timestamp, ZonedDateTime.now()))
    val latestText = formatMeasurementValue(latest.copy(tertiaryValue = null), separator) +
        "\n" + formatTimestamp(latest.timestamp, dateTimeFormatter)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatLine(stringResource(R.string.stats_last_recorded), lastRecorded, Emphasis.NONE)
        StatLine(stringResource(R.string.stats_latest_measurement), latestText, Emphasis.NONE)
    }
}

// "2 days ago" / «πριν από 2 ημέρες», "yesterday" / «χθες», in the app's language,
// written by Android's built-in formatter (ICU).
@Composable
private fun timeAgoText(timeAgo: TimeAgo): String {
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
