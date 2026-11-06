package com.example.healthlog.ui.history.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.chart.stats.NonPeriodStatsSection
import java.time.ZonedDateTime

// The chart page of one type: the period card (stats and chart, see PeriodCard), and the
// latest entry below it, which is always shown, whatever the period.
// Scrolls up and down when it doesn't fit (small screens, landscape).
@Composable
fun ChartPage(
    type: MeasurementType,
    // All the measurements of the type, newest first.
    measurements: List<Measurement>,
    period: ChartPeriod,
    // The latest tap anywhere on the screen, or null before the first one. Passed on to the
    // chart, which uses it to close the box of a tapped dot.
    screenTap: ScreenTap?
) {
    val inPeriod = remember(measurements, period) {
        measurements.inPeriod(period, ZonedDateTime.now())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PeriodCard(type = type, inPeriod = inPeriod, screenTap = screenTap)

        // The list is newest first. A type without any measurement shows only the message.
        measurements.firstOrNull()?.let { latest ->
            NonPeriodStatsSection(latest = latest)
        }
    }
}
