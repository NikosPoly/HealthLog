package com.example.healthlog.ui.history.chart.vico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.evaluateBloodGlucose
import com.example.healthlog.guidelines.evaluateDiastolicStage
import com.example.healthlog.guidelines.evaluateSystolicStage
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.history.chart.ElementPosition
import com.example.healthlog.ui.history.chart.ScreenTap
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
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
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
fun EntryChart(
    type: MeasurementType,
    // The measurements to draw, in any order. Must not be empty.
    measurements: List<Measurement>,
    // The latest tap anywhere on the screen, or null before the first one.
    screenTap: ScreenTap?,
    modifier: Modifier = Modifier,
    latestHeight: Double? = null,
) {
    // When any of these changes, the chart is drawn from scratch, without the box
    // of a tapped dot:
    // - chartInstance is new every time the chart is shown (e.g. after swiping back to it)
    // - measurements change with the period and the data
    // - closeCount goes up on every tap outside the chart
    // - latestHeight changes (for weight healthy target range)
    val chartInstance = remember { Any() }
    var closeCount by remember { mutableIntStateOf(0) }
    val chartPosition = remember { ElementPosition() }

    // Taps on the chart itself are handled by the chart.
    LaunchedEffect(screenTap) {
        if (screenTap != null && !chartPosition.contains(screenTap.position)) closeCount++
    }

    key(chartInstance, measurements, closeCount, latestHeight) {
        VicoLineChart(
            type = type,
            measurements = measurements,
            latestHeight = latestHeight,
            modifier = modifier.onGloballyPositioned { chartPosition.coordinates = it }
        )
    }
}

// The chart itself, drawn with Vico.
@Composable
private fun VicoLineChart(
    type: MeasurementType,
    measurements: List<Measurement>,
    latestHeight: Double?,
    modifier: Modifier
) {
    val oldestFirst = remember(measurements) { measurements.sortedBy { it.timestamp } }
    val model = remember(oldestFirst, type) { chartModel(oldestFirst, type) }

    val field = type.fields[0]
    val separator = decimalSeparator()
    val locale = Locale.forLanguageTag(stringResource(R.string.format_locale))

    // Clinical target ranges (normal pulse zone, healthy weight range).
    val targetRanges = remember(type, latestHeight) {
        targetRangesFor(type, latestHeight)
    }
    val targetDecoration = rememberTargetBandDecoration(type, latestHeight)
    val decorations = remember(targetDecoration) {
        listOfNotNull(targetDecoration)
    }

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
    // Includes target ranges so normal zones are always visible in the chart window.
    val rangeProvider = remember(field.maxDecimals, targetRanges) {
        PaddedRange(
            minMargin = if (field.maxDecimals > 0) 1.0 else 5.0,
            targetRanges = targetRanges,
        )
    }

    val markerController = rememberTapOnDotController()

    // Takes the chart's colors and text styles from the app's Material theme.
    ProvideVicoTheme(rememberM3VicoTheme()) {
        val lines = when (type) {
            MeasurementType.BLOOD_PRESSURE -> rememberBloodPressureLines(oldestFirst)
            MeasurementType.BLOOD_SUGAR -> listOf(rememberGlucoseLine(oldestFirst, MaterialTheme.colorScheme.primary))
            else -> listOf(
                rememberDotLine(MaterialTheme.colorScheme.primary),
                rememberDotLine(MaterialTheme.colorScheme.tertiary),
            )
        }
        val marker = rememberEntryMarker(oldestFirst, latestHeight)

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(lines),
                    rangeProvider = rangeProvider
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
                decorations = decorations,
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

// A line whose dots vary in color (by clinical evaluation) and shape (by meal timing).
// Circle = Before meal / Fasting; Square = 2 hours after meal.
@Composable
private fun rememberGlucoseLine(
    oldestFirst: List<Measurement>,
    color: Color,
): LineCartesianLayer.Line {
    val dark = isSystemInDarkTheme()
    val pointProvider = rememberGlucosePointProvider(oldestFirst, dark)

    return LineCartesianLayer.rememberLine(
        fill = remember(color) { LineCartesianLayer.LineFill.single(Fill(color)) },
        pointProvider = pointProvider,
    )
}

@Composable
private fun rememberGlucosePointProvider(
    oldestFirst: List<Measurement>,
    dark: Boolean,
): LineCartesianLayer.PointProvider {
    val circleShape = CircleShape
    val squareShape = RoundedCornerShape(1.dp)

    val points = remember(dark) {
        val map = mutableMapOf<Pair<BloodGlucoseCategory, MealContext?>, LineCartesianLayer.Point>()
        for (cat in BloodGlucoseCategory.entries) {
            val color = glucoseCategoryColor(cat, dark)
            // Before meal -> Circle
            val circleComponent = ShapeComponent(
                fill = Fill(color),
                shape = circleShape,
            )
            map[Pair(cat, MealContext.BEFORE_MEAL)] = LineCartesianLayer.Point(circleComponent, size = 8.dp)
            map[Pair(cat, null)] = LineCartesianLayer.Point(circleComponent, size = 8.dp)

            // After meal (2h) -> Square
            val squareComponent = ShapeComponent(
                fill = Fill(color),
                shape = squareShape,
            )
            map[Pair(cat, MealContext.AFTER_MEAL_2H)] = LineCartesianLayer.Point(squareComponent, size = 8.dp)
        }
        map
    }

    val measurementByX = remember(oldestFirst) {
        oldestFirst.associateBy { it.timestamp / MILLIS_PER_DAY }
    }

    val fallbackPoint = remember(points) {
        points.values.first()
    }

    return remember(points, measurementByX, fallbackPoint) {
        GlucosePointProvider(measurementByX, points, fallbackPoint)
    }
}

internal class GlucosePointProvider(
    private val measurementByX: Map<Double, Measurement>,
    private val points: Map<Pair<BloodGlucoseCategory, MealContext?>, LineCartesianLayer.Point>,
    private val fallbackPoint: LineCartesianLayer.Point,
) : LineCartesianLayer.PointProvider {
    fun getPoint(entry: LineCartesianLayerModel.Entry): LineCartesianLayer.Point {
        val measurement = measurementByX[entry.x] ?: return fallbackPoint
        val meal = MealContext.fromNameOrNull(measurement.mealContext)
        val category = evaluateBloodGlucose(measurement.value, meal)
        return points[Pair(category, meal)] ?: fallbackPoint
    }

    override fun getPoint(
        entry: LineCartesianLayerModel.Entry,
        extraStore: ExtraStore,
    ): LineCartesianLayer.Point = getPoint(entry)

    override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point = fallbackPoint
}

// Blood pressure lines: Systolic (Circle dots) and Diastolic (Square dots),
// each dot colored by its respective ISH 2020 hypertension stage.
@Composable
private fun rememberBloodPressureLines(
    oldestFirst: List<Measurement>,
): List<LineCartesianLayer.Line> {
    val dark = isSystemInDarkTheme()
    val systolicLineColor = MaterialTheme.colorScheme.primary
    val diastolicLineColor = MaterialTheme.colorScheme.tertiary

    val measurementByX = remember(oldestFirst) {
        oldestFirst.associateBy { it.timestamp / MILLIS_PER_DAY }
    }

    val systolicPoints = remember(dark) {
        BloodPressureStage.entries.associateWith { stage ->
            val color = bloodPressureStageColor(stage, dark)
            val component = ShapeComponent(fill = Fill(color), shape = CircleShape)
            LineCartesianLayer.Point(component, size = 8.dp)
        }
    }

    val diastolicPoints = remember(dark) {
        val squareShape = RoundedCornerShape(1.dp)
        BloodPressureStage.entries.associateWith { stage ->
            val color = bloodPressureStageColor(stage, dark)
            val component = ShapeComponent(fill = Fill(color), shape = squareShape)
            LineCartesianLayer.Point(component, size = 8.dp)
        }
    }

    val systolicFallback = remember(systolicPoints) { systolicPoints.values.first() }
    val diastolicFallback = remember(diastolicPoints) { diastolicPoints.values.first() }

    val systolicPointProvider = remember(measurementByX, systolicPoints, systolicFallback) {
        BloodPressurePointProvider(
            isSystolic = true,
            measurementByX = measurementByX,
            pointsByStage = systolicPoints,
            fallbackPoint = systolicFallback
        )
    }

    val diastolicPointProvider = remember(measurementByX, diastolicPoints, diastolicFallback) {
        BloodPressurePointProvider(
            isSystolic = false,
            measurementByX = measurementByX,
            pointsByStage = diastolicPoints,
            fallbackPoint = diastolicFallback
        )
    }

    return listOf(
        LineCartesianLayer.rememberLine(
            fill = remember(systolicLineColor) { LineCartesianLayer.LineFill.single(Fill(systolicLineColor)) },
            pointProvider = systolicPointProvider,
        ),
        LineCartesianLayer.rememberLine(
            fill = remember(diastolicLineColor) { LineCartesianLayer.LineFill.single(Fill(diastolicLineColor)) },
            pointProvider = diastolicPointProvider,
        ),
    )
}

internal class BloodPressurePointProvider(
    private val isSystolic: Boolean,
    private val measurementByX: Map<Double, Measurement>,
    private val pointsByStage: Map<BloodPressureStage, LineCartesianLayer.Point>,
    private val fallbackPoint: LineCartesianLayer.Point,
) : LineCartesianLayer.PointProvider {
    fun getPoint(entry: LineCartesianLayerModel.Entry): LineCartesianLayer.Point {
        if (entry.x !in measurementByX) return fallbackPoint
        val stage = if (isSystolic) {
            evaluateSystolicStage(entry.y)
        } else {
            evaluateDiastolicStage(entry.y)
        }
        return pointsByStage[stage] ?: fallbackPoint
    }

    override fun getPoint(
        entry: LineCartesianLayerModel.Entry,
        extraStore: ExtraStore,
    ): LineCartesianLayer.Point = getPoint(entry)

    override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point = fallbackPoint
}


