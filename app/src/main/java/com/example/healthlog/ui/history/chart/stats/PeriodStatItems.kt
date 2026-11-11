package com.example.healthlog.ui.history.chart.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator

// The stats of the period, ready to show, in the pairs the period card puts them in.
internal class PeriodStatItems(
    // Above the chart: entries and average.
    val above: Pair<StatItem, StatItem>,
    // Below the chart: highest and lowest. Blood pressure has two pairs: the systolic,
    // then the diastolic.
    val below: List<Pair<StatItem, StatItem>>
)

// Count, highest, lowest and average of the measurements in the period.
// Blood pressure: highest and lowest systolic and diastolic, each with the other value
// recorded with it, and the average of both.
// Values are written as in the history list, without the pulse (it has its own tab).
// In the blood pressure values, the number the stat is about is bold: the systolic in
// "Highest Systolic: 140/80 mmHg", the diastolic in "Highest Diastolic: 125/88 mmHg".
@Composable
internal fun periodStatItems(
    type: MeasurementType,
    // The measurements in the chosen period. Not empty.
    inPeriod: List<Measurement>,
    onOpenDetails: ((Long) -> Unit)? = null
): PeriodStatItems {
    val stats = remember(inPeriod, type) { periodStats(inPeriod, type) }
    val separator = decimalSeparator()

    fun valueText(measurement: Measurement?): String =
        measurement
            ?.let { statValueText(it, separator) }
            ?: NO_VALUE

    fun clickAction(measurement: Measurement?): (() -> Unit)? =
        if (measurement != null && onOpenDetails != null) {
            { onOpenDetails(measurement.id) }
        } else {
            null
        }

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

    val above =
        StatItem(R.drawable.ic_stat_entries, stringResource(R.string.stats_count), stats.count.toString()) to
            StatItem(R.drawable.ic_stat_average, stringResource(R.string.stats_average), average)

    // Blood pressure uses the same up and down icons for the systolic and the diastolic.
    val below = if (type == MeasurementType.BLOOD_PRESSURE) {
        listOf(
            StatItem(
                R.drawable.ic_stat_highest,
                stringResource(R.string.stats_highest_systolic),
                valueText(stats.primary?.highest),
                Emphasis.SYSTOLIC,
                onClick = clickAction(stats.primary?.highest)
            ) to StatItem(
                R.drawable.ic_stat_lowest,
                stringResource(R.string.stats_lowest_systolic),
                valueText(stats.primary?.lowest),
                Emphasis.SYSTOLIC,
                onClick = clickAction(stats.primary?.lowest)
            ),
            StatItem(
                R.drawable.ic_stat_highest,
                stringResource(R.string.stats_highest_diastolic),
                valueText(stats.secondary?.highest),
                Emphasis.DIASTOLIC,
                onClick = clickAction(stats.secondary?.highest)
            ) to StatItem(
                R.drawable.ic_stat_lowest,
                stringResource(R.string.stats_lowest_diastolic),
                valueText(stats.secondary?.lowest),
                Emphasis.DIASTOLIC,
                onClick = clickAction(stats.secondary?.lowest)
            )
        )
    } else {
        listOf(
            StatItem(
                R.drawable.ic_stat_highest,
                stringResource(R.string.stats_highest),
                valueText(stats.primary?.highest),
                onClick = clickAction(stats.primary?.highest)
            ) to StatItem(
                R.drawable.ic_stat_lowest,
                stringResource(R.string.stats_lowest),
                valueText(stats.primary?.lowest),
                onClick = clickAction(stats.primary?.lowest)
            )
        )
    }

    return PeriodStatItems(above = above, below = below)
}

// Shown when there's no value. The stats are only shown with measurements, so this is
// only a safeguard.
private const val NO_VALUE = "-"
