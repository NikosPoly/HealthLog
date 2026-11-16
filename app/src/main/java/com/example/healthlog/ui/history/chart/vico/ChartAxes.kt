package com.example.healthlog.ui.history.chart.vico

import android.text.format.DateFormat
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

// The chart's axes: the x values are times in days, the bottom axis shows them as dates,
// and the value axis fits the measurements.

// One day in milliseconds: the unit of the chart's x values.
internal const val MILLIS_PER_DAY = 86_400_000.0

// The dates on the bottom axis, in the app's language: e.g. "Oct 5" or «5 Οκτ».
internal class DateAxisFormatter(locale: Locale, skeleton: String) : CartesianValueFormatter {
    private val formatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

    override fun format(
        context: CartesianMeasuringContext,
        value: Double,
        verticalAxisPosition: Axis.Position.Vertical?
    ): CharSequence =
        formatter.format(
            Instant.ofEpochMilli((value * MILLIS_PER_DAY).roundToLong())
                .atZone(ZoneId.systemDefault())
        )
}

// The value axis fits the measurements with some room above and below, instead of
// starting at 0, so small changes (e.g. in weight) are visible.
// When target ranges (e.g. normal clinical thresholds) are present, the axis bounds
// comfortably include them as well.
internal class PaddedRange(
    private val minMargin: Double,
    private val targetRanges: List<ClosedFloatingPointRange<Double>> = emptyList(),
) : CartesianLayerRangeProvider {
    fun getMinY(minY: Double, maxY: Double): Double {
        val effectiveMinY = if (targetRanges.isEmpty()) minY else minOf(minY, targetRanges.minOf { it.start })
        val effectiveMaxY = if (targetRanges.isEmpty()) maxY else maxOf(maxY, targetRanges.maxOf { it.endInclusive })
        return floor(effectiveMinY - margin(effectiveMinY, effectiveMaxY)).coerceAtLeast(0.0)
    }

    fun getMaxY(minY: Double, maxY: Double): Double {
        val effectiveMinY = if (targetRanges.isEmpty()) minY else minOf(minY, targetRanges.minOf { it.start })
        val effectiveMaxY = if (targetRanges.isEmpty()) maxY else maxOf(maxY, targetRanges.maxOf { it.endInclusive })
        return ceil(effectiveMaxY + margin(effectiveMinY, effectiveMaxY))
    }

    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        getMinY(minY, maxY)

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        getMaxY(minY, maxY)

    private fun margin(minY: Double, maxY: Double) = max((maxY - minY) * 0.1, minMargin)
}
