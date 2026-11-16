package com.example.healthlog.ui.history.chart.vico

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.guidelines.evaluateDiastolicStage
import com.example.healthlog.guidelines.evaluateSystolicStage
import com.example.healthlog.ui.details.references.bloodGlucoseCategoryColors
import com.example.healthlog.ui.details.references.bloodPressureStageColors
import com.example.healthlog.ui.details.references.bmiCategoryColors
import com.example.healthlog.ui.details.references.pulseZoneColors
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementType
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetRangeBandTest {

    @Test
    fun targetRangesFor_pulseReturnsRestingNormalRange() {
        val ranges = targetRangesFor(MeasurementType.PULSE)
        assertEquals(1, ranges.size)
        assertEquals(60.0, ranges[0].start, 0.001)
        assertEquals(100.0, ranges[0].endInclusive, 0.001)
    }

    @Test
    fun targetRangesFor_weightWithHeightReturnsHealthyRange() {
        val ranges = targetRangesFor(MeasurementType.WEIGHT, latestHeightCm = 180.0)
        assertEquals(1, ranges.size)
        val h2 = 1.8 * 1.8
        assertEquals(18.5 * h2, ranges[0].start, 0.001)
        assertEquals(24.9 * h2, ranges[0].endInclusive, 0.001)
    }

    @Test
    fun targetRangesFor_weightWithoutHeightReturnsEmpty() {
        assertTrue(targetRangesFor(MeasurementType.WEIGHT, latestHeightCm = null).isEmpty())
        assertTrue(targetRangesFor(MeasurementType.WEIGHT, latestHeightCm = 0.0).isEmpty())
    }

    @Test
    fun targetRangesFor_otherTypesReturnEmpty() {
        assertTrue(targetRangesFor(MeasurementType.BLOOD_PRESSURE).isEmpty())
        assertTrue(targetRangesFor(MeasurementType.BLOOD_SUGAR).isEmpty())
        assertTrue(targetRangesFor(MeasurementType.HEIGHT).isEmpty())
    }

    @Test
    fun stageBandsFor_pulseReturnsThreeClinicalBands() {
        val bands = stageBandsFor(MeasurementType.PULSE, dark = false)
        assertEquals(3, bands.size)

        // Bradycardia (< 60 bpm)
        assertEquals(0.0, bands[0].range.start, 0.001)
        assertEquals(60.0, bands[0].range.endInclusive, 0.001)

        // Normal resting (60–100 bpm)
        assertEquals(60.0, bands[1].range.start, 0.001)
        assertEquals(100.0, bands[1].range.endInclusive, 0.001)

        // Tachycardia (> 100 bpm) - extends to infinity
        assertEquals(100.0, bands[2].range.start, 0.001)
        assertEquals(Double.POSITIVE_INFINITY, bands[2].range.endInclusive, 0.0)
    }

    @Test
    fun stageBandsFor_weightWithHeightReturnsFourBmiBands() {
        val heightCm = 175.0
        val bands = stageBandsFor(MeasurementType.WEIGHT, dark = false, latestHeightCm = heightCm)
        assertEquals(4, bands.size)

        val h2 = 1.75 * 1.75

        // Underweight (BMI < 18.5)
        assertEquals(0.0, bands[0].range.start, 0.001)
        assertEquals(18.5 * h2, bands[0].range.endInclusive, 0.001)

        // Normal (BMI 18.5–24.9)
        assertEquals(18.5 * h2, bands[1].range.start, 0.001)
        assertEquals(25.0 * h2, bands[1].range.endInclusive, 0.001)

        // Overweight (BMI 25.0–29.9)
        assertEquals(25.0 * h2, bands[2].range.start, 0.001)
        assertEquals(30.0 * h2, bands[2].range.endInclusive, 0.001)

        // Obese (BMI ≥ 30.0) - extends to infinity
        assertEquals(30.0 * h2, bands[3].range.start, 0.001)
        assertEquals(Double.POSITIVE_INFINITY, bands[3].range.endInclusive, 0.0)
    }

    @Test
    fun stageBandsFor_weightWithoutHeightReturnsEmpty() {
        assertTrue(stageBandsFor(MeasurementType.WEIGHT, dark = false, latestHeightCm = null).isEmpty())
        assertTrue(stageBandsFor(MeasurementType.WEIGHT, dark = false, latestHeightCm = -10.0).isEmpty())
    }

    @Test
    fun stageBandsFor_otherTypesReturnEmpty() {
        assertTrue(stageBandsFor(MeasurementType.BLOOD_PRESSURE, dark = false).isEmpty())
        assertTrue(stageBandsFor(MeasurementType.BLOOD_SUGAR, dark = false).isEmpty())
        assertTrue(stageBandsFor(MeasurementType.HEIGHT, dark = false).isEmpty())
    }

    @Test
    fun paddedRange_anchorsAroundTargetRange() {
        // Data is only 70..75 bpm, but target is 60..100 bpm
        val targetRanges = listOf(60.0..100.0)
        val provider = PaddedRange(minMargin = 5.0, targetRanges = targetRanges)

        val minY = provider.getMinY(minY = 70.0, maxY = 75.0)
        val maxY = provider.getMaxY(minY = 70.0, maxY = 75.0)

        // Min must be <= 60 - margin
        assertTrue("minY should be <= 60.0", minY <= 60.0)
        // Max must be >= 100 + margin
        assertTrue("maxY should be >= 100.0", maxY >= 100.0)
    }

    @Test
    fun stageLabelResFor_resolvesPulseStages() {
        val brady = Measurement(type = MeasurementType.PULSE, value = 55.0)
        assertEquals(R.string.pulse_zone_bradycardia, stageLabelResFor(brady))

        val normal = Measurement(type = MeasurementType.PULSE, value = 72.0)
        assertEquals(R.string.pulse_zone_normal, stageLabelResFor(normal))

        val tachy = Measurement(type = MeasurementType.PULSE, value = 110.0)
        assertEquals(R.string.pulse_zone_tachycardia, stageLabelResFor(tachy))
    }

    @Test
    fun stageLabelResFor_resolvesWeightStagesWithHeight() {
        val heightCm = 180.0 // Normal weight is approx 60..81 kg
        val normalWeight = Measurement(type = MeasurementType.WEIGHT, value = 72.0)
        assertEquals(R.string.bmi_category_normal, stageLabelResFor(normalWeight, heightCm))

        val obeseWeight = Measurement(type = MeasurementType.WEIGHT, value = 110.0)
        assertEquals(R.string.bmi_category_obese, stageLabelResFor(obeseWeight, heightCm))

        // Without height, weight stage cannot be evaluated
        assertNull(stageLabelResFor(normalWeight, null))
    }

    @Test
    fun stageLabelResFor_resolvesBloodPressureAndBloodSugarStages() {
        val bp = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
        )
        assertNotNull(stageLabelResFor(bp))

        val sugarFasting = Measurement(
            type = MeasurementType.BLOOD_SUGAR,
            value = 95.0,
            mealContext = MealContext.BEFORE_MEAL.name,
        )
        assertNotNull(stageLabelResFor(sugarFasting))
    }

    @Test
    fun glucoseCategoryColor_mapsToUniversalPalette() {
        val dark = false
        assertEquals(Color(0xFF1E88E5), glucoseCategoryColor(BloodGlucoseCategory.HYPOGLYCEMIA, dark))
        assertEquals(Color(0xFF4CAF50), glucoseCategoryColor(BloodGlucoseCategory.NORMAL, dark))
        assertEquals(Color(0xFFFB8C00), glucoseCategoryColor(BloodGlucoseCategory.PREDIABETES, dark))
        assertEquals(Color(0xFFE53935), glucoseCategoryColor(BloodGlucoseCategory.DIABETES, dark))
    }

    @Test
    fun glucosePointProvider_resolvesShapesAndColorsByMealContextAndEvaluation() {
        val time1 = 1000L * 86_400_000L
        val time2 = 1001L * 86_400_000L
        val m1 = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_SUGAR,
            value = 130.0,
            mealContext = MealContext.BEFORE_MEAL.name,
            timestamp = time1,
        )
        val m2 = Measurement(
            id = 2,
            type = MeasurementType.BLOOD_SUGAR,
            value = 130.0,
            mealContext = MealContext.AFTER_MEAL_2H.name,
            timestamp = time2,
        )
        val measurements = listOf(m1, m2)
        val measurementByX = measurements.associateBy { it.timestamp / MILLIS_PER_DAY }

        val beforePoint = LineCartesianLayer.Point(
            component = ShapeComponent(fill = Fill(Color.Red), shape = CircleShape),
            size = 8.dp,
        )
        val afterPoint = LineCartesianLayer.Point(
            component = ShapeComponent(fill = Fill(Color.Green), shape = RoundedCornerShape(1.dp)),
            size = 8.dp,
        )
        val points: Map<Pair<BloodGlucoseCategory, MealContext?>, LineCartesianLayer.Point> = mapOf(
            Pair(BloodGlucoseCategory.DIABETES, MealContext.BEFORE_MEAL) to beforePoint,
            Pair(BloodGlucoseCategory.NORMAL, MealContext.AFTER_MEAL_2H) to afterPoint,
        )

        val provider = GlucosePointProvider(measurementByX, points, beforePoint)

        // 130 before meal evaluates to DIABETES -> returns beforePoint (Circle, Red)
        val point1 = provider.getPoint(LineCartesianLayerModel.Entry(1000.0, 130.0))
        assertEquals(beforePoint, point1)

        // 130 after meal evaluates to NORMAL -> returns afterPoint (Square, Green)
        val point2 = provider.getPoint(LineCartesianLayerModel.Entry(1001.0, 130.0))
        assertEquals(afterPoint, point2)
    }

    @Test
    fun evaluateSystolicAndDiastolicStages_distinguishIndependentThresholds() {
        // 80 systolic is LOW (< 90), but 80 diastolic is NORMAL (< 85)
        assertEquals(BloodPressureStage.LOW, evaluateSystolicStage(80.0))
        assertEquals(BloodPressureStage.NORMAL, evaluateDiastolicStage(80.0))

        // 135 systolic is HIGH_NORMAL (130..139), but 135 diastolic is CRISIS (> 120)
        assertEquals(BloodPressureStage.HIGH_NORMAL, evaluateSystolicStage(135.0))
        assertEquals(BloodPressureStage.HYPERTENSIVE_CRISIS, evaluateDiastolicStage(135.0))
    }

    @Test
    fun bloodPressureStageColor_mapsToUniversalPalette() {
        val dark = false
        assertEquals(Color(0xFF0288D1), bloodPressureStageColor(BloodPressureStage.LOW, dark))
        assertEquals(Color(0xFF2E7D32), bloodPressureStageColor(BloodPressureStage.NORMAL, dark))
        assertEquals(Color(0xFFF57F17), bloodPressureStageColor(BloodPressureStage.HIGH_NORMAL, dark))
        assertEquals(Color(0xFFE64A19), bloodPressureStageColor(BloodPressureStage.GRADE_1_HYPERTENSION, dark))
        assertEquals(Color(0xFFD32F2F), bloodPressureStageColor(BloodPressureStage.GRADE_2_HYPERTENSION, dark))
        assertEquals(Color(0xFFC2185B), bloodPressureStageColor(BloodPressureStage.HYPERTENSIVE_CRISIS, dark))
    }

    @Test
    fun bloodPressurePointProvider_systolicCircleAndDiastolicSquareResolution() {
        val time1 = 1000L * 86_400_000L
        val bp = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 80.0,           // Systolic = 80 (Low)
            secondaryValue = 80.0,  // Diastolic = 80 (Normal)
            timestamp = time1,
        )
        val measurementByX = mapOf((time1 / MILLIS_PER_DAY) to bp)

        val lowCircle = LineCartesianLayer.Point(
            component = ShapeComponent(fill = Fill(Color.Blue), shape = CircleShape),
            size = 8.dp,
        )
        val normalCircle = LineCartesianLayer.Point(
            component = ShapeComponent(fill = Fill(Color.Green), shape = CircleShape),
            size = 8.dp,
        )
        val normalSquare = LineCartesianLayer.Point(
            component = ShapeComponent(fill = Fill(Color.Green), shape = RoundedCornerShape(1.dp)),
            size = 8.dp,
        )

        val systolicPoints = mapOf(
            BloodPressureStage.LOW to lowCircle,
            BloodPressureStage.NORMAL to normalCircle,
        )
        val diastolicPoints = mapOf(
            BloodPressureStage.NORMAL to normalSquare,
        )

        val systolicProvider = BloodPressurePointProvider(
            isSystolic = true,
            measurementByX = measurementByX,
            pointsByStage = systolicPoints,
            fallbackPoint = normalCircle,
        )
        val diastolicProvider = BloodPressurePointProvider(
            isSystolic = false,
            measurementByX = measurementByX,
            pointsByStage = diastolicPoints,
            fallbackPoint = normalSquare,
        )

        // Systolic 80 mmHg -> resolves to LOW (lowCircle: Circle, Blue)
        val sysPoint = systolicProvider.getPoint(LineCartesianLayerModel.Entry(1000.0, 80.0))
        assertEquals(lowCircle, sysPoint)

        // Diastolic 80 mmHg -> resolves to NORMAL (normalSquare: Square, Green)
        val diaPoint = diastolicProvider.getPoint(LineCartesianLayerModel.Entry(1000.0, 80.0))
        assertEquals(normalSquare, diaPoint)
    }

    @Test
    fun stageColorFor_resolvesConditionColorsAcrossAllTypes() {
        val dark = false

        // Blood pressure (120/80) -> Normal -> normal stage colors
        val bp = Measurement(type = MeasurementType.BLOOD_PRESSURE, value = 120.0, secondaryValue = 80.0)
        assertEquals(bloodPressureStageColors(BloodPressureStage.NORMAL, dark), stageColorFor(bp, dark))

        // Blood glucose (fasting 135) -> Diabetes -> diabetes stage colors
        val glucose = Measurement(
            type = MeasurementType.BLOOD_SUGAR,
            value = 135.0,
            mealContext = MealContext.BEFORE_MEAL.name,
        )
        assertEquals(bloodGlucoseCategoryColors(BloodGlucoseCategory.DIABETES, dark), stageColorFor(glucose, dark))

        // Pulse (50) -> Bradycardia -> bradycardia stage colors
        val brady = Measurement(type = MeasurementType.PULSE, value = 50.0)
        assertEquals(pulseZoneColors(PulseZone.BRADYCARDIA, dark), stageColorFor(brady, dark))

        // Weight with height (72 kg, 180 cm) -> Normal BMI -> normal BMI colors
        val weight = Measurement(type = MeasurementType.WEIGHT, value = 72.0)
        assertEquals(bmiCategoryColors(BmiCategory.NORMAL, dark), stageColorFor(weight, dark, latestHeightCm = 180.0))

        // Weight without height -> null (fallback to theme surface)
        assertNull(stageColorFor(weight, dark, latestHeightCm = null))

        // Height -> null (fallback to theme surface)
        val height = Measurement(type = MeasurementType.HEIGHT, value = 180.0)
        assertNull(stageColorFor(height, dark))

        // Dark theme test
        val darkTheme = true
        assertEquals(bloodPressureStageColors(BloodPressureStage.NORMAL, darkTheme), stageColorFor(bp, darkTheme))
        assertEquals(pulseZoneColors(PulseZone.BRADYCARDIA, darkTheme), stageColorFor(brady, darkTheme))
    }
}
