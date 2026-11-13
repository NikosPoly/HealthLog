package com.example.healthlog.guidelines

import kotlin.math.pow

// WHO International Body Mass Index classification categories.
enum class BmiCategory {
    UNDERWEIGHT,    // BMI < 18.5
    NORMAL,         // BMI 18.5–24.9
    OVERWEIGHT,     // BMI 25.0–29.9
    OBESE           // BMI ≥ 30.0
}

// Full evaluation of weight relative to height, including traditional WHO BMI,
// Oxford New BMI, healthy weight target range, and distance to normal range.
data class BmiEvaluation(
    val bmi: Double,
    val newBmi: Double,
    val category: BmiCategory,
    val heightCm: Double,
    val weightKg: Double,
    val healthyWeightMinKg: Double,
    val healthyWeightMaxKg: Double,
    val weightDeltaKg: Double,
) {
    // True when weight falls within the WHO normal range (18.5–24.9).
    val isHealthyWeight: Boolean
        get() = category == BmiCategory.NORMAL
}

// Calculates healthy weight bounds (in kg) for a given height based on WHO normal BMI (18.5–24.9).
fun calculateHealthyWeightRange(heightCm: Double): ClosedFloatingPointRange<Double>? {
    if (heightCm <= 0.0) return null
    val heightM = heightCm / 100.0
    val minWeight = 18.5 * (heightM * heightM)
    val maxWeight = 24.9 * (heightM * heightM)
    return minWeight..maxWeight
}

// Evaluates weight and height into WHO BMI, Oxford New BMI, and healthy target range.
fun calculateBmi(weightKg: Double, heightCm: Double): BmiEvaluation? {
    if ((weightKg <= 0.0) || (heightCm <= 0.0)) return null
    val heightM = heightCm / 100.0

    // Standard WHO Quetelet formula: weight / height^2
    val bmi = weightKg / (heightM * heightM)

    // Oxford University (Nick Trefethen) formula: 1.3 * weight / height^2.5
    val newBmi = 1.3 * weightKg / heightM.pow(2.5)

    val category = when {
        bmi < 18.5 -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.NORMAL
        bmi < 30.0 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    val healthyMin = 18.5 * (heightM * heightM)
    val healthyMax = 24.9 * (heightM * heightM)

    val delta = when {
        weightKg < healthyMin -> weightKg - healthyMin
        weightKg > healthyMax -> weightKg - healthyMax
        else -> 0.0
    }

    return BmiEvaluation(
        bmi = bmi,
        newBmi = newBmi,
        category = category,
        heightCm = heightCm,
        weightKg = weightKg,
        healthyWeightMinKg = healthyMin,
        healthyWeightMaxKg = healthyMax,
        weightDeltaKg = delta
    )
}
