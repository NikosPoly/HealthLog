package com.example.healthlog.ui.history.chart.vico

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.evaluateMeasurement
import com.example.healthlog.ui.details.references.labelRes
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.MarkerCornerBasedShape
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
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

// The box shown when a dot is tapped: how it looks, what it says, and when it opens.
// Used by EntryChart; it must be called inside its ProvideVicoTheme.

// The box's text: the value as in the history list, then clinical stage (if applicable), then date and time.
// Blood pressure shows systolic and diastolic together (with the pulse).
@Composable
internal fun rememberEntryMarker(
    // The chart's measurements, oldest first.
    oldestFirst: List<Measurement>,
    latestHeight: Double? = null,
): DefaultCartesianMarker {
    val resources = LocalResources.current
    val separator = decimalSeparator()
    val dateTimeFormatter = rememberDateTimeFormatter()
    val markerFormatter = remember(oldestFirst, separator, dateTimeFormatter, latestHeight) {
        val measurementAt = oldestFirst.associateBy { it.timestamp / MILLIS_PER_DAY }
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val measurement = measurementAt[targets.first().x]
            if (measurement == null) {
                ""
            } else {
                val valueText = formatMeasurementValue(measurement, separator)
                val stageText = stageLabelResFor(measurement, latestHeight)?.let {
                    resources.getString(it)
                }
                val timeText = formatTimestamp(measurement.timestamp, dateTimeFormatter)

                if (stageText != null) {
                    "$valueText\n$stageText\n$timeText"
                } else {
                    "$valueText\n$timeText"
                }
            }
        }
    }

    return rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
            lineCount = 3,
            padding = Insets(8.dp, 4.dp),
            background = rememberShapeComponent(
                fill = Fill(MaterialTheme.colorScheme.surface),
                shape = MarkerCornerBasedShape(RoundedCornerShape(8.dp)),
                strokeFill = Fill(MaterialTheme.colorScheme.outline),
                strokeThickness = 1.dp
            )
        ),
        valueFormatter = markerFormatter,
        // Above the dot, or below it when there's no room above.
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
        // A vertical line through the tapped dot.
        guideline = rememberAxisGuidelineComponent()
    )
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
