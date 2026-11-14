package com.example.healthlog.guidelines

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementType

// Result of evaluating a measurement against international clinical guidelines.
sealed interface GuidelineEvaluation {

    // Blood pressure evaluation based on ISH 2020 guidelines, including optional pulse.
    data class BloodPressure(
        val stage: BloodPressureStage,
        val systolic: Double,
        val diastolic: Double,
        val pulseEvaluation: Pulse? = null,
    ) : GuidelineEvaluation

    // Blood glucose evaluation based on WHO / IDF criteria.
    data class BloodGlucose(
        val category: BloodGlucoseCategory,
        val value: Double,
        val mealContext: MealContext?,
    ) : GuidelineEvaluation

    // Resting heart rate evaluation based on WHF / WHO consensus.
    data class Pulse(
        val zone: PulseZone,
        val rate: Double
    ) : GuidelineEvaluation

    // Weight and BMI evaluation based on WHO criteria and Oxford New BMI formula.
    data class Weight(
        val weightKg: Double,
        val bmiEvaluation: BmiEvaluation?
    ) : GuidelineEvaluation

    // Height reference guidelines with healthy weight target range for this height.
    data class Height(
        val heightCm: Double,
        val healthyWeightMinKg: Double,
        val healthyWeightMaxKg: Double
    ) : GuidelineEvaluation
}

// Pure evaluator that takes a measurement (and optional latest recorded height for weight entries)
// and returns the corresponding clinical guideline evaluation.
fun evaluateMeasurement(
    measurement: Measurement,
    latestHeightCm: Double? = null
): GuidelineEvaluation? {
    return when (measurement.type) {
        MeasurementType.BLOOD_PRESSURE -> {
            val diastolic = measurement.secondaryValue ?: return null
            val systolic = measurement.value
            val pulseEval = measurement.tertiaryValue?.let {
                GuidelineEvaluation.Pulse(evaluatePulse(it), it)
            }
            GuidelineEvaluation.BloodPressure(
                stage = evaluateBloodPressure(systolic, diastolic),
                systolic = systolic,
                diastolic = diastolic,
                pulseEvaluation = pulseEval
            )
        }
        MeasurementType.PULSE -> {
            val rate = measurement.value
            GuidelineEvaluation.Pulse(
                zone = evaluatePulse(rate),
                rate = rate
            )
        }
        MeasurementType.BLOOD_SUGAR -> {
            val meal = MealContext.fromNameOrNull(measurement.mealContext)
            GuidelineEvaluation.BloodGlucose(
                category = evaluateBloodGlucose(measurement.value, meal),
                value = measurement.value,
                mealContext = meal,
            )
        }
        MeasurementType.WEIGHT -> {
            val bmiEval = if ((latestHeightCm != null) && (latestHeightCm > 0.0)) {
                calculateBmi(measurement.value, latestHeightCm)
            } else {
                null
            }
            GuidelineEvaluation.Weight(
                weightKg = measurement.value,
                bmiEvaluation = bmiEval
            )
        }
        MeasurementType.HEIGHT -> {
            val heightCm = measurement.value
            val range = calculateHealthyWeightRange(heightCm) ?: return null
            GuidelineEvaluation.Height(
                heightCm = heightCm,
                healthyWeightMinKg = range.start,
                healthyWeightMaxKg = range.endInclusive
            )
        }
    }
}
