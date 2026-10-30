package com.example.healthlog.ui.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import java.time.ZonedDateTime

// The trend page of one type: its chart and, later, its stats.
// For now only a placeholder where the chart will go, with the number of measurements
// in the chosen period.
@Composable
fun ChartPage(
    type: MeasurementType,
    // All the measurements of the type, newest first.
    measurements: List<Measurement>,
    period: ChartPeriod
) {
    val inPeriod = remember(measurements, period) {
        measurements.inPeriod(period, ZonedDateTime.now())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.chart_description, stringResource(type.label)))
        // Temporary, to check the chips until the chart and stats are added.
        Text("${stringResource(R.string.stats_count)}: ${inPeriod.size}")
    }
}
