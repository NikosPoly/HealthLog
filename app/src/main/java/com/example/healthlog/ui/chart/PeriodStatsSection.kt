package com.example.healthlog.ui.chart

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator

// The stats below the chart.
// "In this period": count, highest, lowest and average of the measurements in the period.
// Blood pressure: highest and lowest systolic and diastolic, each with the other value
// recorded with it, and the average of both.
// Values are written as in the history list, without the pulse (it has its own tab).
// In the blood pressure rows, the number the row is about is bold: the systolic in
// "Highest systolic: 140/80 mmHg", the diastolic in "Highest diastolic: 125/88 mmHg".
@Composable
fun PeriodStatsSection(
    type: MeasurementType,
    // The measurements in the chosen period. Not empty.
    inPeriod: List<Measurement>,
    modifier: Modifier = Modifier
) {
    val stats = remember(inPeriod, type) { periodStats(inPeriod, type) }
    val separator = decimalSeparator()

    fun valueText(measurement: Measurement?): String =
        measurement
            ?.let { statValueText(it, separator) }
            ?: NO_VALUE

    // The average is written like a measurement of the same type, e.g. "72.46 kg" or
    // "130/85 mmHg".
    val average = valueText(
        stats.primary?.let { primary ->
            Measurement(
                type = type,
                value = primary.average,
                secondaryValue = stats.secondary?.average
            )
        }
    )

    val rows = buildList {
        add(StatRow(R.string.stats_count, stats.count.toString()))

        if (type == MeasurementType.BLOOD_PRESSURE) {
            add(StatRow(R.string.stats_highest_systolic, valueText(stats.primary?.highest), Emphasis.SYSTOLIC))
            add(StatRow(R.string.stats_lowest_systolic, valueText(stats.primary?.lowest), Emphasis.SYSTOLIC))
            add(StatRow(R.string.stats_highest_diastolic, valueText(stats.secondary?.highest), Emphasis.DIASTOLIC))
            add(StatRow(R.string.stats_lowest_diastolic, valueText(stats.secondary?.lowest), Emphasis.DIASTOLIC))
        } else {
            add(StatRow(R.string.stats_highest, valueText(stats.primary?.highest)))
            add(StatRow(R.string.stats_lowest, valueText(stats.primary?.lowest)))
        }

        add(StatRow(R.string.stats_average, average))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_period_title),
            style = MaterialTheme.typography.titleMedium
        )

        rows.forEach { row ->
            StatLine(label = stringResource(row.label), value = row.value, emphasis = row.emphasis)
        }
    }
}

// Shown when there's no value. The section is only shown with measurements, so this is
// only a safeguard.
private const val NO_VALUE = "-"

private class StatRow(
    @param:StringRes val label: Int,
    val value: String,
    val emphasis: Emphasis = Emphasis.NONE
)
