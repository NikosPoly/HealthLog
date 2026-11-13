package com.example.healthlog.guidelines

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidelineEvaluationTest {

    @Test
    fun bloodPressure_withoutPulse_evaluatesCorrectly() {
        val measurement = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0
        )
        val evaluation = evaluateMeasurement(measurement)
        assertTrue(evaluation is GuidelineEvaluation.BloodPressure)
        val bp = evaluation as GuidelineEvaluation.BloodPressure
        assertEquals(BloodPressureStage.NORMAL, bp.stage)
        assertEquals(120.0, bp.systolic, 0.001)
        assertEquals(80.0, bp.diastolic, 0.001)
        assertNull(bp.pulseEvaluation)
    }

    @Test
    fun bloodPressure_withPulse_evaluatesBothBpAndPulse() {
        val measurement = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            tertiaryValue = 105.0
        )
        val evaluation = evaluateMeasurement(measurement)
        assertTrue(evaluation is GuidelineEvaluation.BloodPressure)
        val bp = evaluation as GuidelineEvaluation.BloodPressure
        assertEquals(BloodPressureStage.GRADE_1_HYPERTENSION, bp.stage)
        val pulseEval = bp.pulseEvaluation
        assertNotNull(pulseEval)
        assertEquals(PulseZone.TACHYCARDIA, pulseEval!!.zone)
        assertEquals(105.0, pulseEval.rate, 0.001)
    }

    @Test
    fun pulse_evaluatesCorrectly() {
        val measurement = Measurement(
            type = MeasurementType.PULSE,
            value = 55.0
        )
        val evaluation = evaluateMeasurement(measurement)
        assertTrue(evaluation is GuidelineEvaluation.Pulse)
        val pulse = evaluation as GuidelineEvaluation.Pulse
        assertEquals(PulseZone.BRADYCARDIA, pulse.zone)
        assertEquals(55.0, pulse.rate, 0.001)
    }

    @Test
    fun bloodGlucose_evaluatesWithMealContext() {
        val measurement = Measurement(
            type = MeasurementType.BLOOD_SUGAR,
            value = 110.0,
            mealContext = MealContext.BEFORE_MEAL.name
        )
        val evaluation = evaluateMeasurement(measurement)
        assertTrue(evaluation is GuidelineEvaluation.BloodGlucose)
        val glucose = evaluation as GuidelineEvaluation.BloodGlucose
        assertEquals(BloodGlucoseCategory.PREDIABETES, glucose.category)
        assertEquals(MealContext.BEFORE_MEAL, glucose.mealContext)
    }

    @Test
    fun weight_withoutKnownHeight_hasNullBmiEvaluation() {
        val measurement = Measurement(
            type = MeasurementType.WEIGHT,
            value = 75.0
        )
        val evaluation = evaluateMeasurement(measurement, latestHeightCm = null)
        assertTrue(evaluation is GuidelineEvaluation.Weight)
        val weight = evaluation as GuidelineEvaluation.Weight
        assertEquals(75.0, weight.weightKg, 0.001)
        assertNull(weight.bmiEvaluation)
    }

    @Test
    fun weight_withKnownHeight_evaluatesBmi() {
        val measurement = Measurement(
            type = MeasurementType.WEIGHT,
            value = 72.0
        )
        val evaluation = evaluateMeasurement(measurement, latestHeightCm = 175.0)
        assertTrue(evaluation is GuidelineEvaluation.Weight)
        val weight = evaluation as GuidelineEvaluation.Weight
        assertNotNull(weight.bmiEvaluation)
        assertEquals(BmiCategory.NORMAL, weight.bmiEvaluation!!.category)
    }

    @Test
    fun height_evaluatesHealthyWeightRange() {
        val measurement = Measurement(
            type = MeasurementType.HEIGHT,
            value = 180.0
        )
        val evaluation = evaluateMeasurement(measurement)
        assertTrue(evaluation is GuidelineEvaluation.Height)
        val height = evaluation as GuidelineEvaluation.Height
        assertEquals(180.0, height.heightCm, 0.001)
        assertEquals(18.5 * 1.8 * 1.8, height.healthyWeightMinKg, 0.001)
        assertEquals(24.9 * 1.8 * 1.8, height.healthyWeightMaxKg, 0.001)
    }
}
