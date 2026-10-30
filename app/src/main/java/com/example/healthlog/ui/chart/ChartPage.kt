package com.example.healthlog.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import java.time.ZonedDateTime

// The trend page of one type: its chart and, later, its stats.
// Without measurements in the period, a message instead of the chart.
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (inPeriod.isEmpty()) {
            Text(stringResource(R.string.chart_empty))
        } else {
            // Read aloud by screen readers, e.g. "Chart of Weight".
            val description = stringResource(R.string.chart_description, stringResource(type.label))

            MeasurementChart(
                type = type,
                measurements = inPeriod,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .semantics { contentDescription = description }
            )
        }

        // Temporary, to check the chips until the stats are added.
        Text("${stringResource(R.string.stats_count)}: ${inPeriod.size}")
    }
}
