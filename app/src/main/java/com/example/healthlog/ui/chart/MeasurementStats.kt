package com.example.healthlog.ui.chart

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementField
import com.example.healthlog.model.MeasurementType
import java.math.BigDecimal
import java.math.RoundingMode

// The stats of one value over a period, e.g. the systolic.
data class FieldStats(
    // The whole measurement, so blood pressure keeps its pair:
    // the highest systolic together with the diastolic recorded with it.
    // On a tie, the most recent one.
    val highest: Measurement,
    val lowest: Measurement,
    // Already rounded (see roundForStats).
    val average: Double
)

// The stats that depend on the chosen period.
data class PeriodStats(
    val count: Int,
    // The main value: systolic for blood pressure.
    // Null with fewer than two measurements; the screen shows "-".
    val primary: FieldStats?,
    // Diastolic for blood pressure; null for the other types.
    // The pulse has its own stats in the Pulse tab.
    val secondary: FieldStats?
)

fun periodStats(measurements: List<Measurement>, type: MeasurementType): PeriodStats =
    PeriodStats(
        count = measurements.size,
        primary = fieldStats(measurements, type.fields[0]) { it.value },
        secondary = if (type == MeasurementType.BLOOD_PRESSURE) {
            fieldStats(measurements, type.fields[1]) { it.secondaryValue }
        } else {
            null
        }
    )

private fun fieldStats(
    measurements: List<Measurement>,
    field: MeasurementField,
    valueOf: (Measurement) -> Double?
): FieldStats? {
    val withValues = measurements.mapNotNull { measurement ->
        valueOf(measurement)?.let { value -> measurement to value }
    }
    if (withValues.size < 2) return null

    val highest = withValues.maxWith(
        compareBy<Pair<Measurement, Double>> { it.second }.thenBy { it.first.timestamp }
    )
    val lowest = withValues.minWith(
        compareBy<Pair<Measurement, Double>> { it.second }.thenByDescending { it.first.timestamp }
    )
    val average = withValues.map { it.second }.average()

    return FieldStats(
        highest = highest.first,
        lowest = lowest.first,
        average = roundForStats(average, field.maxDecimals)
    )
}

// Types with decimals are rounded to their decimals (weight: 72.456 → 72.46).
// Whole-number types drop the decimals (132.9 → 132).
internal fun roundForStats(value: Double, maxDecimals: Int): Double {
    val mode = if (maxDecimals > 0) RoundingMode.HALF_UP else RoundingMode.DOWN
    return BigDecimal.valueOf(value).setScale(maxDecimals, mode).toDouble()
}
