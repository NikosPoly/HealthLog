package com.example.healthlog.ui.history.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.chart.stats.StatPair
import com.example.healthlog.ui.history.chart.stats.periodStatItems
import com.example.healthlog.ui.history.chart.vico.EntryChart

// Everything that depends on the chosen period, in one rounded card: entries and average
// above the chart, highest and lowest below it.
// Without measurements in the period, only the message, without the card around it.
@Composable
fun PeriodCard(
    type: MeasurementType,
    // The measurements in the chosen period.
    inPeriod: List<Measurement>,
    // Passed on to the chart (see ChartPage).
    screenTap: ScreenTap?,
    modifier: Modifier = Modifier,
    onOpenDetails: ((Long) -> Unit)? = null
) {
    if (inPeriod.isEmpty()) {
        Text(stringResource(R.string.chart_empty), modifier = modifier)
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        // Rounder than the other cards, to set the period apart. Still being tried out.
        shape = RoundedCornerShape(24.dp),
        // A strong tint of the app's main color, so the period stands out on the page.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val items = periodStatItems(type, inPeriod, onOpenDetails)
            // Read aloud by screen readers, e.g. "Chart of Weight".
            val description = stringResource(R.string.chart_description, stringResource(type.label))

            StatPair(items.above.first, items.above.second)

            EntryChart(
                type = type,
                measurements = inPeriod,
                screenTap = screenTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .semantics { contentDescription = description }
            )

            items.below.forEach { (start, end) ->
                StatPair(start, end)
            }
        }
    }
}
