package com.example.healthlog.ui.history.chart.stats

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
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

// Percentage and ratio distribution of a condition (e.g. Left arm: 3 of 4, 75%).
data class ConditionDistribution<T>(
    val value: T,
    val count: Int,
    val total: Int,
    val percentage: Int,
    val ratio: Float
)

// Summary of arrhythmia detection frequency.
data class ArrhythmiaStats(
    val detectedCount: Int,
    val totalRecorded: Int,
    val percentage: Int,
    val ratio: Float
)

// Condition breakdown statistics over a period.
data class ConditionBreakdown(
    val armDistribution: List<ConditionDistribution<MeasurementArm>> = emptyList(),
    val positionDistribution: List<ConditionDistribution<BodyPosition>> = emptyList(),
    val mealDistribution: List<ConditionDistribution<MealContext>> = emptyList(),
    val arrhythmiaStats: ArrhythmiaStats? = null
) {
    val isEmpty: Boolean
        get() = armDistribution.isEmpty() &&
            positionDistribution.isEmpty() &&
            mealDistribution.isEmpty() &&
            arrhythmiaStats == null
}

// The stats that depend on the chosen period.
data class PeriodStats(
    val count: Int,
    // The main value: systolic for blood pressure.
    // Null without measurements. With one, it is the highest, the lowest and the average.
    val primary: FieldStats?,
    // Diastolic for blood pressure; null for the other types.
    // The pulse has its own stats in the Pulse tab.
    val secondary: FieldStats?,
    // Clinical conditions recorded in the period (arm, posture, meals, arrhythmia).
    val conditions: ConditionBreakdown = ConditionBreakdown()
)

fun periodStats(measurements: List<Measurement>, type: MeasurementType): PeriodStats =
    PeriodStats(
        count = measurements.size,
        primary = fieldStats(measurements, type.fields[0]) { it.value },
        secondary = if (type == MeasurementType.BLOOD_PRESSURE) {
            fieldStats(measurements, type.fields[1]) { it.secondaryValue }
        } else {
            null
        },
        conditions = periodConditionStats(measurements, type)
    )

fun periodConditionStats(measurements: List<Measurement>, type: MeasurementType): ConditionBreakdown {
    if (measurements.isEmpty()) return ConditionBreakdown()

    return when (type) {
        MeasurementType.BLOOD_PRESSURE -> {
            val arms = calculateDistribution(
                measurements.mapNotNull { MeasurementArm.fromNameOrNull(it.arm) }
            )
            val positions = calculateDistribution(
                measurements.mapNotNull { BodyPosition.fromNameOrNull(it.bodyPosition) }
            )
            val arrhythmia = calculateArrhythmiaStats(measurements)
            ConditionBreakdown(
                armDistribution = arms,
                positionDistribution = positions,
                arrhythmiaStats = arrhythmia
            )
        }
        MeasurementType.BLOOD_SUGAR -> {
            val meals = calculateDistribution(
                measurements.mapNotNull { MealContext.fromNameOrNull(it.mealContext) }
            )
            ConditionBreakdown(mealDistribution = meals)
        }
        MeasurementType.PULSE -> {
            val arrhythmia = calculateArrhythmiaStats(measurements)
            ConditionBreakdown(arrhythmiaStats = arrhythmia)
        }
        MeasurementType.WEIGHT, MeasurementType.HEIGHT -> {
            ConditionBreakdown()
        }
    }
}

private fun <T> calculateDistribution(items: List<T>): List<ConditionDistribution<T>> {
    if (items.isEmpty()) return emptyList()
    val total = items.size
    val counts = items.groupingBy { it }.eachCount()
    return counts.entries
        .sortedWith(
            compareByDescending<Map.Entry<T, Int>> { it.value }
                .thenBy { if (it.key is Enum<*>) (it.key as Enum<*>).ordinal else 0 }
        )
        .map { (value, count) ->
            val ratio = count.toFloat() / total
            val percentage = Math.round((count.toDouble() / total) * 100).toInt()
            ConditionDistribution(
                value = value,
                count = count,
                total = total,
                percentage = percentage,
                ratio = ratio
            )
        }
}

private fun calculateArrhythmiaStats(measurements: List<Measurement>): ArrhythmiaStats? {
    val recorded = measurements.mapNotNull { it.hasArrhythmia }
    if (recorded.isEmpty()) return null
    val total = recorded.size
    val detected = recorded.count { it }
    val ratio = detected.toFloat() / total
    val percentage = Math.round((detected.toDouble() / total) * 100).toInt()
    return ArrhythmiaStats(
        detectedCount = detected,
        totalRecorded = total,
        percentage = percentage,
        ratio = ratio
    )
}

private fun fieldStats(
    measurements: List<Measurement>,
    field: MeasurementField,
    valueOf: (Measurement) -> Double?
): FieldStats? {
    val withValues = measurements.mapNotNull { measurement ->
        valueOf(measurement)?.let { value -> measurement to value }
    }
    if (withValues.isEmpty()) return null

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
