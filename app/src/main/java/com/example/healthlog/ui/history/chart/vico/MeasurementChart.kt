package com.example.healthlog.ui.history.chart.vico

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.chart.ElementPosition
import com.example.healthlog.ui.history.chart.ScreenTap
import com.example.healthlog.ui.format.decimalSeparator
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
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
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import java.util.Locale

// The line chart of one type: a dot per measurement, joined in time order.
// Blood pressure has two lines, systolic and diastolic; its pulse is in the Pulse chart.
// The bottom axis is time: the space between two dots matches the time between them.
// Scrolling and zooming are off, so the whole period always fits and a sideways swipe
// over the chart changes the tab.
// Tapping a dot shows its value, date and time; tapping it again or anywhere else hides it.
// The box closes on a tap anywhere on the screen outside the chart (screenTap), and it
// never comes back by itself: not after swiping to another tab and back, nor after
// choosing another period.
// The Vico chart library is only used here and in ChartTooltip.kt (the tap box) and
// ChartAxes.kt (the axes).
@Composable
fun MeasurementChart(
    type: MeasurementType,
    // The measurements to draw, in any order. Must not be empty.
    measurements: List<Measurement>,
    // The latest tap anywhere on the screen, or null before the first one.
    screenTap: ScreenTap?,
    modifier: Modifier = Modifier
) {
    // When any of these three changes, the chart is drawn from scratch, without the box
    // of a tapped dot:
    // - chartInstance is new every time the chart is shown (e.g. after swiping back to it)
    // - measurements change with the period and the data
    // - closeCount goes up on every tap outside the chart
    val chartInstance = remember { Any() }
    var closeCount by remember { mutableIntStateOf(0) }
    val chartPosition = remember { ElementPosition() }

    // Taps on the chart itself are handled by the chart.
    LaunchedEffect(screenTap) {
        if (screenTap != null && !chartPosition.contains(screenTap.position)) closeCount++
    }

    key(chartInstance, measurements, closeCount) {
        VicoLineChart(
            type = type,
            measurements = measurements,
            modifier = modifier.onGloballyPositioned { chartPosition.coordinates = it }
        )
    }
}

// The chart itself, drawn with Vico.
@Composable
private fun VicoLineChart(
    type: MeasurementType,
    measurements: List<Measurement>,
    modifier: Modifier
) {
    val oldestFirst = remember(measurements) { measurements.sortedBy { it.timestamp } }
    val model = remember(oldestFirst, type) { chartModel(oldestFirst, type) }

    val field = type.fields[0]
    val separator = decimalSeparator()
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
            decimalSeparator = separator.toString()
        )
    }
    // Whole-number types get a wider minimum range, so their axis steps stay whole numbers.
    val rangeProvider = remember(field.maxDecimals) {
        PaddedRange(minMargin = if (field.maxDecimals > 0) 1.0 else 5.0)
    }

    val markerController = rememberTapOnDotController()

    // Takes the chart's colors and text styles from the app's Material theme.
    ProvideVicoTheme(rememberM3VicoTheme()) {
        val lines = listOf(
            rememberDotLine(MaterialTheme.colorScheme.primary),
            rememberDotLine(MaterialTheme.colorScheme.tertiary)
        )
        val marker = rememberMeasurementMarker(oldestFirst)

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
                getXStep = { _, _, _ -> 1.0 },
                marker = marker,
                markerController = markerController
            ),
            model = model,
            modifier = modifier,
            scrollState = rememberVicoScrollState(scrollEnabled = false)
        )
    }
}

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
