package com.example.healthlog.ui.chart

import android.text.format.DateFormat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

// The line chart of one type: a dot per measurement, joined in time order.
// Blood pressure has two lines, systolic and diastolic; its pulse is in the Pulse chart.
// The bottom axis is time: the space between two dots matches the time between them.
// Scrolling and zooming are off, so the whole period always fits and a sideways swipe
// over the chart changes the tab.
// This is the only file that uses the Vico chart library.
@Composable
fun MeasurementChart(
    type: MeasurementType,
    // The measurements to draw, in any order. Must not be empty.
    measurements: List<Measurement>,
    modifier: Modifier = Modifier
) {
    val oldestFirst = remember(measurements) { measurements.sortedBy { it.timestamp } }
    val model = remember(oldestFirst, type) { chartModel(oldestFirst, type) }

    val field = type.fields[0]
    val separator = decimalSeparator().toString()
    val locale = Locale.forLanguageTag(stringResource(R.string.format_locale))

    // Long periods show month and year on the axis; shorter ones day and month.
    val longPeriod = oldestFirst.last().timestamp - oldestFirst.first().timestamp >
        LONG_PERIOD_DAYS * MILLIS_PER_DAY
    val xFormatter = remember(locale, longPeriod) {
        DateAxisFormatter(locale, if (longPeriod) "MMMyyyy" else "dMMM")
    }
    val yFormatter = remember(field.maxDecimals, separator) {
        CartesianValueFormatter.decimal(
            decimalCount = field.maxDecimals,
            decimalSeparator = separator
        )
    }
    // Whole-number types get a wider minimum range, so their axis steps stay whole numbers.
    val rangeProvider = remember(field.maxDecimals) {
        PaddedRange(minMargin = if (field.maxDecimals > 0) 1.0 else 5.0)
    }

    // Takes the chart's colors and text styles from the app's Material theme.
    ProvideVicoTheme(rememberM3VicoTheme()) {
        val lines = listOf(
            rememberDotLine(MaterialTheme.colorScheme.primary),
            rememberDotLine(MaterialTheme.colorScheme.tertiary)
        )

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(lines),
                    rangeProvider = rangeProvider
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
                // One day: the base unit of the time axis.
                // (The lambda receives the model and the x range, which aren't needed.)
                getXStep = { _, _, _ -> 1.0 }
            ),
            model = model,
            modifier = modifier,
            scrollState = rememberVicoScrollState(scrollEnabled = false)
        )
    }
}

private const val MILLIS_PER_DAY = 86_400_000.0
private const val LONG_PERIOD_DAYS = 180

// The chart's data. x is the time in days, y the value.
private fun chartModel(oldestFirst: List<Measurement>, type: MeasurementType): CartesianChartModel =
    CartesianChartModel(
        LineCartesianLayerModel.build {
            series(
                x = oldestFirst.map { it.timestamp / MILLIS_PER_DAY },
                y = oldestFirst.map { it.value }
            )

            if (type == MeasurementType.BLOOD_PRESSURE) {
                val withDiastolic = oldestFirst.filter { it.secondaryValue != null }
                if (withDiastolic.isNotEmpty()) {
                    series(
                        x = withDiastolic.map { it.timestamp / MILLIS_PER_DAY },
                        y = withDiastolic.map { it.secondaryValue!! }
                    )
                }
            }
        }
    )

// A line in one color, with a dot on every measurement.
@Composable
private fun rememberDotLine(color: Color): LineCartesianLayer.Line {
    val dot = rememberShapeComponent(fill = Fill(color), shape = CircleShape)

    return LineCartesianLayer.rememberLine(
        fill = remember(color) { LineCartesianLayer.LineFill.single(Fill(color)) },
        pointProvider = remember(dot) {
            LineCartesianLayer.PointProvider.single(LineCartesianLayer.Point(dot, size = 8.dp))
        }
    )
}

// The dates on the bottom axis, in the app's language: e.g. "Oct 5" or «5 Οκτ».
private class DateAxisFormatter(locale: Locale, skeleton: String) : CartesianValueFormatter {
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
private class PaddedRange(private val minMargin: Double) : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        floor(minY - margin(minY, maxY)).coerceAtLeast(0.0)

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        ceil(maxY + margin(minY, maxY))

    private fun margin(minY: Double, maxY: Double) = max((maxY - minY) * 0.1, minMargin)
}
