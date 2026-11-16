package com.example.healthlog.ui.history.chart.vico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.guidelines.calculateHealthyWeightRange
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.details.references.StageColor
import com.example.healthlog.ui.details.references.bloodGlucoseCategoryColors
import com.example.healthlog.ui.details.references.bloodPressureStageColors
import com.example.healthlog.ui.details.references.bmiCategoryColors
import com.example.healthlog.ui.details.references.pulseZoneColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration

// A clinical stage band with a numerical range, background fill color, and boundary stroke color.
data class StageBand(
    val range: ClosedFloatingPointRange<Double>,
    val fillColor: Color,
    val strokeColor: Color? = null,
)

// Clinical normal target ranges used to anchor the chart's vertical range in PaddedRange.
// Configured specifically for Weight and Pulse.
fun targetRangesFor(
    type: MeasurementType,
    latestHeightCm: Double? = null,
): List<ClosedFloatingPointRange<Double>> {
    return when (type) {
        // WHF / WHO: Resting pulse normal 60–100 bpm
        MeasurementType.PULSE -> listOf(60.0..100.0)
        // WHO: Normal BMI 18.5–24.9 calculated for user's recorded height
        MeasurementType.WEIGHT -> {
            if ((latestHeightCm != null) && (latestHeightCm > 0.0)) {
                calculateHealthyWeightRange(latestHeightCm)?.let { listOf(it) } ?: emptyList()
            } else {
                emptyList()
            }
        }
        // Background bands are intentionally omitted for other types
        MeasurementType.BLOOD_PRESSURE,
        MeasurementType.BLOOD_SUGAR,
        MeasurementType.HEIGHT -> emptyList()
    }
}

// Universal clinical color for blood glucose evaluation categories (matches details screen reference table).
fun glucoseCategoryColor(category: BloodGlucoseCategory, dark: Boolean): Color =
    bloodGlucoseCategoryColors(category, dark).indicator

// Universal clinical color for blood pressure stages (matches details screen reference table).
fun bloodPressureStageColor(stage: BloodPressureStage, dark: Boolean): Color =
    bloodPressureStageColors(stage, dark).indicator

// Universal clinical color for resting pulse zones (matches details screen reference table).
fun pulseZoneColor(zone: PulseZone, dark: Boolean): Color =
    pulseZoneColors(zone, dark).indicator

// Universal clinical color for WHO BMI categories (matches details screen reference table).
fun bmiCategoryColor(category: BmiCategory, dark: Boolean): Color =
    bmiCategoryColors(category, dark).indicator

// Clinical stage bands for Weight and Pulse using the canonical reference palette:
fun stageBandsFor(
    type: MeasurementType,
    dark: Boolean,
    latestHeightCm: Double? = null,
): List<StageBand> {
    val defaultFillAlpha = if (dark) 0.22f else 0.16f
    val defaultStrokeAlpha = if (dark) 0.50f else 0.35f

    // Vivid alphas for alert bands (orange overweight and red tachycardia/obese) so they don't look faint/pinkish
    val orangeFillAlpha = if (dark) 0.38f else 0.34f
    val orangeStrokeAlpha = if (dark) 0.80f else 0.70f

    val redFillAlpha = if (dark) 0.42f else 0.38f
    val redStrokeAlpha = if (dark) 0.85f else 0.75f

    fun band(
        range: ClosedFloatingPointRange<Double>,
        stageColor: StageColor,
        fillAlpha: Float = defaultFillAlpha,
        strokeAlpha: Float = defaultStrokeAlpha,
    ) = StageBand(
        range = range,
        fillColor = stageColor.indicator.copy(alpha = fillAlpha),
        strokeColor = stageColor.indicator.copy(alpha = strokeAlpha),
    )

    return when (type) {
        MeasurementType.PULSE -> listOf(
            band(0.0..60.0, pulseZoneColors(PulseZone.BRADYCARDIA, dark)),
            band(60.0..100.0, pulseZoneColors(PulseZone.NORMAL, dark)),
            band(
                100.0..Double.POSITIVE_INFINITY,
                pulseZoneColors(PulseZone.TACHYCARDIA, dark),
                fillAlpha = redFillAlpha,
                strokeAlpha = redStrokeAlpha,
            ),
        )
        MeasurementType.WEIGHT -> {
            if ((latestHeightCm != null) && (latestHeightCm > 0.0)) {
                val heightM = latestHeightCm / 100.0
                val h2 = heightM * heightM
                listOf(
                    band(0.0..(18.5 * h2), bmiCategoryColors(BmiCategory.UNDERWEIGHT, dark)),
                    band((18.5 * h2)..(25.0 * h2), bmiCategoryColors(BmiCategory.NORMAL, dark)),
                    band(
                        (25.0 * h2)..(30.0 * h2),
                        bmiCategoryColors(BmiCategory.OVERWEIGHT, dark),
                        fillAlpha = orangeFillAlpha,
                        strokeAlpha = orangeStrokeAlpha,
                    ),
                    band(
                        (30.0 * h2)..Double.POSITIVE_INFINITY,
                        bmiCategoryColors(BmiCategory.OBESE, dark),
                        fillAlpha = redFillAlpha,
                        strokeAlpha = redStrokeAlpha,
                    ),
                )
            } else {
                emptyList()
            }
        }
        MeasurementType.BLOOD_PRESSURE,
        MeasurementType.BLOOD_SUGAR,
        MeasurementType.HEIGHT -> emptyList()
    }
}

// A Vico chart decoration that renders horizontal semi-transparent clinical stage bands
// behind the chart layers, giving users instant visual reference of healthy and elevated ranges.
class TargetBandDecoration(
    val bands: List<StageBand>,
    val strokeThickness: Dp = 1.dp,
) : Decoration {

    private val fillPaint = Paint().apply {
        style = PaintingStyle.Fill
    }

    private val strokePaint = Paint().apply {
        style = PaintingStyle.Stroke
    }

    override fun drawUnderLayers(context: CartesianDrawingContext) {
        with(context) {
            val yRange = ranges.getYRange(null)
            if (yRange.length <= 0) return

            strokePaint.strokeWidth = strokeThickness.pixels

            for ((range, fillColor, strokeColor) in bands) {
                val topY = if (range.endInclusive.isInfinite()) {
                    layerBounds.top
                } else {
                    layerBounds.bottom -
                        (((range.endInclusive - yRange.minY) / yRange.length).toFloat() * layerBounds.height)
                }
                val bottomY = if ((range.start <= 0.0) || range.start.isInfinite()) {
                    layerBounds.bottom
                } else {
                    layerBounds.bottom -
                        (((range.start - yRange.minY) / yRange.length).toFloat() * layerBounds.height)
                }

                val clampedTop = topY.coerceIn(layerBounds.top, layerBounds.bottom)
                val clampedBottom = bottomY.coerceIn(layerBounds.top, layerBounds.bottom)

                if (clampedTop < clampedBottom) {
                    fillPaint.color = fillColor
                    canvas.drawRect(
                        Rect(layerBounds.left, clampedTop, layerBounds.right, clampedBottom),
                        fillPaint,
                    )
                }

                if (strokeColor != null) {
                    strokePaint.color = strokeColor
                    // Only draw inner boundary lines, never artificial borders at infinity or zero
                    if (range.endInclusive.isFinite() && (topY >= layerBounds.top) && (topY <= layerBounds.bottom)) {
                        canvas.drawLine(
                            Offset(layerBounds.left, topY),
                            Offset(layerBounds.right, topY),
                            strokePaint,
                        )
                    }
                    if (range.start.isFinite() && (range.start > 0.0) && (bottomY >= layerBounds.top) && (bottomY <= layerBounds.bottom)) {
                        canvas.drawLine(
                            Offset(layerBounds.left, bottomY),
                            Offset(layerBounds.right, bottomY),
                            strokePaint,
                        )
                    }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (
            (other is TargetBandDecoration) &&
                (bands == other.bands) &&
                (strokeThickness == other.strokeThickness)
        )
    }

    override fun hashCode(): Int {
        var result = bands.hashCode()
        result = (31 * result) + strokeThickness.hashCode()
        return result
    }
}

// Remembers a theme-aware TargetBandDecoration for Weight and Pulse.
@Composable
fun rememberTargetBandDecoration(
    type: MeasurementType,
    latestHeight: Double? = null,
): Decoration? {
    val dark = isSystemInDarkTheme()
    val bands = remember(type, dark, latestHeight) {
        stageBandsFor(type, dark, latestHeight)
    }
    if (bands.isEmpty()) return null

    return remember(bands) {
        TargetBandDecoration(
            bands = bands,
            strokeThickness = 1.dp,
        )
    }
}
