package com.example.healthlog.ui.history.chart.vico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.evaluateMeasurement
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.details.references.StageColor
import com.example.healthlog.ui.details.references.bloodGlucoseCategoryColors
import com.example.healthlog.ui.details.references.bloodPressureStageColors
import com.example.healthlog.ui.details.references.bmiCategoryColors
import com.example.healthlog.ui.details.references.labelRes
import com.example.healthlog.ui.details.references.pulseZoneColors
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerMargins
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.MarkerCornerBasedShape
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import kotlin.math.abs

// Resolves the string resource ID for the clinical stage of a measurement.
fun stageLabelResFor(measurement: Measurement, latestHeightCm: Double? = null): Int? {
    return when (val evaluation = evaluateMeasurement(measurement, latestHeightCm)) {
        is GuidelineEvaluation.BloodPressure -> evaluation.stage.labelRes
        is GuidelineEvaluation.BloodGlucose -> evaluation.category.labelRes
        is GuidelineEvaluation.Pulse -> evaluation.zone.labelRes
        is GuidelineEvaluation.Weight -> evaluation.bmiEvaluation?.category?.labelRes
        is GuidelineEvaluation.Height, null -> null
    }
}

// Resolves the theme-aware colors for the clinical stage of a measurement, or null if unclassified.
fun stageColorFor(
    measurement: Measurement,
    dark: Boolean,
    latestHeightCm: Double? = null,
): StageColor? {
    return when (val evaluation = evaluateMeasurement(measurement, latestHeightCm)) {
        is GuidelineEvaluation.BloodPressure -> bloodPressureStageColors(evaluation.stage, dark)
        is GuidelineEvaluation.BloodGlucose -> bloodGlucoseCategoryColors(evaluation.category, dark)
        is GuidelineEvaluation.Pulse -> pulseZoneColors(evaluation.zone, dark)
        is GuidelineEvaluation.Weight -> evaluation.bmiEvaluation?.let { bmiCategoryColors(it.category, dark) }
        is GuidelineEvaluation.Height, null -> null
    }
}

// Creates a DefaultCartesianMarker styled with specific text and background/border colors.
private fun createMarker(
    textColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    formatter: DefaultCartesianMarker.ValueFormatter,
    guideline: LineComponent?,
): DefaultCartesianMarker {
    return DefaultCartesianMarker(
        label = TextComponent(
            textStyle = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
            lineCount = 3,
            padding = Insets(8.dp, 4.dp),
            background = ShapeComponent(
                fill = Fill(backgroundColor),
                shape = MarkerCornerBasedShape(RoundedCornerShape(8.dp)),
                strokeFill = Fill(borderColor),
                strokeThickness = 1.dp
            )
        ),
        valueFormatter = formatter,
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
        guideline = guideline
    )
}

// A CartesianMarker wrapper that selects a stage-colored DefaultCartesianMarker based on the tapped measurement.
internal class StageAwareCartesianMarker(
    private val defaultMarker: DefaultCartesianMarker,
    private val markerForMeasurement: (Measurement?) -> DefaultCartesianMarker,
    private val measurementAt: (Double) -> Measurement?,
) : CartesianMarker {

    override fun updateLayerMargins(
        context: CartesianMeasuringContext,
        layerMargins: CartesianLayerMargins,
        layerDimensions: CartesianLayerDimensions,
        model: CartesianChartModel,
    ) {
        defaultMarker.updateLayerMargins(context, layerMargins, layerDimensions, model)
    }

    override fun drawUnderLayers(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ) {
        val targetX = targets.firstOrNull()?.x
        val measurement = targetX?.let(measurementAt)
        val marker = markerForMeasurement(measurement)
        marker.drawUnderLayers(context, targets)
    }

    override fun drawOverLayers(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ) {
        val targetX = targets.firstOrNull()?.x
        val measurement = targetX?.let(measurementAt)
        val marker = markerForMeasurement(measurement)
        marker.drawOverLayers(context, targets)
    }
}

// The box shown when a dot is tapped: how it looks, what it says, and when it opens.
// Used by EntryChart; it must be called inside its ProvideVicoTheme.

// The box's text: the value as in the history list, then clinical stage (if applicable), then date and time.
// Blood pressure shows systolic and diastolic together (with the pulse).
@Composable
internal fun rememberEntryMarker(
    // The chart's measurements, oldest first.
    oldestFirst: List<Measurement>,
    latestHeight: Double? = null,
): CartesianMarker {
    val resources = LocalResources.current
    val separator = decimalSeparator()
    val dateTimeFormatter = rememberDateTimeFormatter()
    val dark = isSystemInDarkTheme()
    val defaultTextColor = MaterialTheme.colorScheme.onSurface
    val defaultBgColor = MaterialTheme.colorScheme.surface
    val defaultBorderColor = MaterialTheme.colorScheme.outline
    val guideline = rememberAxisGuidelineComponent()

    return remember(
        oldestFirst,
        separator,
        dateTimeFormatter,
        latestHeight,
        dark,
        defaultTextColor,
        defaultBgColor,
        defaultBorderColor,
        guideline
    ) {
        val measurementAt = oldestFirst.associateBy { it.timestamp / MILLIS_PER_DAY }
        val markerFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
            val measurement = measurementAt[targets.first().x]
            if (measurement == null) {
                ""
            } else {
                val valueText = formatMeasurementValue(measurement, separator)
                val stageText = stageLabelResFor(measurement, latestHeight)?.let {
                    resources.getString(it)
                }
                val mealText = if (measurement.type == MeasurementType.BLOOD_SUGAR) {
                    when (MealContext.fromNameOrNull(measurement.mealContext)) {
                        MealContext.BEFORE_MEAL -> resources.getString(R.string.meal_before)
                        MealContext.AFTER_MEAL_2H -> resources.getString(R.string.meal_after_2h)
                        null -> null
                    }
                } else {
                    null
                }
                val timeText = formatTimestamp(measurement.timestamp, dateTimeFormatter)

                val line2 = when {
                    stageText != null && mealText != null -> "$mealText • $stageText"
                    stageText != null -> stageText
                    mealText != null -> mealText
                    else -> null
                }

                if (line2 != null) {
                    "$valueText\n$line2\n$timeText"
                } else {
                    "$valueText\n$timeText"
                }
            }
        }

        val defaultMarker = createMarker(
            textColor = defaultTextColor,
            backgroundColor = defaultBgColor,
            borderColor = defaultBorderColor,
            formatter = markerFormatter,
            guideline = guideline
        )

        val markerCache = mutableMapOf<StageColor, DefaultCartesianMarker>()
        fun markerFor(measurement: Measurement?): DefaultCartesianMarker {
            val stageColor = measurement?.let { stageColorFor(it, dark, latestHeight) }
                ?: return defaultMarker
            return markerCache.getOrPut(stageColor) {
                createMarker(
                    textColor = stageColor.content,
                    backgroundColor = stageColor.container,
                    borderColor = stageColor.indicator,
                    formatter = markerFormatter,
                    guideline = guideline
                )
            }
        }

        StageAwareCartesianMarker(
            defaultMarker = defaultMarker,
            markerForMeasurement = ::markerFor,
            measurementAt = measurementAt::get
        )
    }
}

// When the box opens and closes on taps on the chart (see TapOnDotController).
@Composable
internal fun rememberTapOnDotController(): CartesianMarkerController {
    val touchRadius = with(LocalDensity.current) { TOUCH_RADIUS.toPx() }
    return remember(touchRadius) { TapOnDotController(touchRadius) }
}

// How close to a dot (sideways) a tap must be to select it.
private val TOUCH_RADIUS = 24.dp

// Shows the marker when a tap lands on a dot; hides it when the same dot is tapped again
// or the tap is away from every dot. (Vico's own "toggle on tap" would instead select the
// nearest dot wherever you tap.)
private class TapOnDotController(private val touchRadius: Float) : CartesianMarkerController {
    private var shownTargets: List<CartesianMarker.Target>? = null

    override val acceptsLongPress = false

    override fun shouldAcceptInteraction(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>
    ) = interaction is Interaction.Tap

    override fun shouldShowMarker(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>
    ): Boolean {
        val onDot = targets.isNotEmpty() &&
            targets.all { abs(it.canvasX - interaction.point.x) <= touchRadius }
        val show = onDot && targets != shownTargets
        shownTargets = if (show) targets else null
        return show
    }
}
