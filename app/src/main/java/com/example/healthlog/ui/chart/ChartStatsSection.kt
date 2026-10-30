package com.example.healthlog.ui.chart

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue

// The stats below the chart.
// "In this period": count, highest, lowest and average of the measurements in the period.
// Blood pressure: highest and lowest systolic and diastolic, each with the other value
// recorded with it, and the average of both.
// Values are written as in the history list, without the pulse (it has its own tab).
// In the blood pressure rows, the number the row is about is bold: the systolic in
// "Highest systolic: 140/80 mmHg", the diastolic in "Highest diastolic: 125/88 mmHg".
@Composable
fun ChartStatsSection(
    type: MeasurementType,
    // The measurements in the chosen period. Not empty.
    inPeriod: List<Measurement>,
    modifier: Modifier = Modifier
) {
    val stats = remember(inPeriod, type) { periodStats(inPeriod, type) }
    val separator = decimalSeparator()

    fun valueText(measurement: Measurement?): String =
        measurement
            ?.let { formatMeasurementValue(it.copy(tertiaryValue = null), separator) }
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

// Which number of a blood pressure value ("140/80 mmHg") is bold.
private enum class Emphasis { NONE, SYSTOLIC, DIASTOLIC }

// One stat: the name on the left, the value on the right.
// Values are medium weight; a value with an emphasis is regular, with that number bold,
// so it stands out.
@Composable
private fun StatLine(label: String, value: String, emphasis: Emphasis) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = buildAnnotatedString {
                append(value)
                emphasizedRange(value, emphasis)?.let { range ->
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first, range.last + 1)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasis == Emphasis.NONE) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// Where the emphasized number is in a blood pressure text: before the "/" for the
// systolic, between the "/" and the space for the diastolic. Null for other texts.
private fun emphasizedRange(value: String, emphasis: Emphasis): IntRange? {
    val slash = value.indexOf('/')
    if (emphasis == Emphasis.NONE || slash < 0) return null

    return when (emphasis) {
        Emphasis.SYSTOLIC -> 0 until slash
        Emphasis.DIASTOLIC -> {
            val space = value.indexOf(' ', startIndex = slash).takeIf { it >= 0 } ?: value.length
            (slash + 1) until space
        }
        Emphasis.NONE -> null
    }
}
