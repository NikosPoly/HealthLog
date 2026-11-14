package com.example.healthlog.guidelines

import com.example.healthlog.model.MealContext

// Blood glucose categories according to the World Health Organization (WHO) and
// the International Diabetes Federation (IDF).
enum class BloodGlucoseCategory {
    HYPOGLYCEMIA,   // < 70 mg/dL
    NORMAL,         // Fasting 70–99 mg/dL | 2h Post-meal < 140 mg/dL
    PREDIABETES,    // Fasting 100–125 mg/dL (IFG) | 2h Post-meal 140–199 mg/dL (IGT)
    DIABETES        // Fasting ≥ 126 mg/dL | 2h Post-meal ≥ 200 mg/dL
}

// Evaluates a blood glucose reading (in mg/dL) against WHO/IDF clinical guidelines,
// taking into account the measurement's meal timing context.
fun evaluateBloodGlucose(value: Double, mealContext: MealContext?): BloodGlucoseCategory {
    if (value < 70.0) {
        return BloodGlucoseCategory.HYPOGLYCEMIA
    }
    return when (mealContext) {
        MealContext.BEFORE_MEAL -> when {
            value >= 126.0 -> BloodGlucoseCategory.DIABETES
            value >= 100.0 -> BloodGlucoseCategory.PREDIABETES
            else -> BloodGlucoseCategory.NORMAL
        }
        MealContext.AFTER_MEAL_2H -> when {
            value >= 200.0 -> BloodGlucoseCategory.DIABETES
            value >= 140.0 -> BloodGlucoseCategory.PREDIABETES
            else -> BloodGlucoseCategory.NORMAL
        }
        null -> when {
            value >= 200.0 -> BloodGlucoseCategory.DIABETES
            value >= 140.0 -> BloodGlucoseCategory.PREDIABETES
            else -> BloodGlucoseCategory.NORMAL
        }
    }
}
