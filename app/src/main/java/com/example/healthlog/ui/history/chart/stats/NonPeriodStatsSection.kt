package com.example.healthlog.ui.history.chart.stats

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
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import com.example.healthlog.ui.format.timeAgo
import com.example.healthlog.ui.format.timeAgoText
import java.time.ZonedDateTime

// The stats that don't depend on the period, below the period stats (or below the
// "needs measurements" message): when the last measurement was recorded, e.g. "2 days ago",
// and that measurement with its date and time.
@Composable
fun NonPeriodStatsSection(
    // The most recent measurement of the type.
    latest: Measurement,
    modifier: Modifier = Modifier
) {
    val separator = decimalSeparator()
    val dateTimeFormatter = rememberDateTimeFormatter()
    val lastRecorded = timeAgoText(timeAgo(latest.timestamp, ZonedDateTime.now()))
    val latestText = statValueText(latest, separator) +
        "\n" + formatTimestamp(latest.timestamp, dateTimeFormatter)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatLine(stringResource(R.string.stats_last_recorded), lastRecorded, Emphasis.NONE)
        StatLine(stringResource(R.string.stats_latest_measurement), latestText, Emphasis.NONE)
    }
}
