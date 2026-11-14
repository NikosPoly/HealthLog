package com.example.healthlog.guidelines

import com.example.healthlog.model.MealContext
import org.junit.Assert.assertEquals
import org.junit.Test

class BloodGlucoseGuidelinesTest {

    // ---- Hypoglycemia (< 70 mg/dL regardless of meal timing) ----

    @Test
    fun hypoglycemia_fastingReadingBelow70() {
        assertEquals(BloodGlucoseCategory.HYPOGLYCEMIA, evaluateBloodGlucose(69.0, MealContext.BEFORE_MEAL))
    }

    @Test
    fun hypoglycemia_postMealReadingBelow70() {
        assertEquals(BloodGlucoseCategory.HYPOGLYCEMIA, evaluateBloodGlucose(55.0, MealContext.AFTER_MEAL_2H))
    }

    @Test
    fun hypoglycemia_unspecifiedMealContextBelow70() {
        assertEquals(BloodGlucoseCategory.HYPOGLYCEMIA, evaluateBloodGlucose(60.0, null))
    }

    // ---- Fasting / Before Meal (WHO/IDF: Normal 70–99, Prediabetes 100–125, Diabetes ≥ 126) ----

    @Test
    fun fasting_normalAtBoundaries() {
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(70.0, MealContext.BEFORE_MEAL))
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(85.0, MealContext.BEFORE_MEAL))
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(99.0, MealContext.BEFORE_MEAL))
    }

    @Test
    fun fasting_prediabetesAtBoundaries() {
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(100.0, MealContext.BEFORE_MEAL))
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(115.0, MealContext.BEFORE_MEAL))
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(125.0, MealContext.BEFORE_MEAL))
    }

    @Test
    fun fasting_diabetesAtOrAbove126() {
        assertEquals(BloodGlucoseCategory.DIABETES, evaluateBloodGlucose(126.0, MealContext.BEFORE_MEAL))
        assertEquals(BloodGlucoseCategory.DIABETES, evaluateBloodGlucose(180.0, MealContext.BEFORE_MEAL))
    }

    // ---- 2h Post-Meal (WHO/IDF: Normal < 140, Prediabetes 140–199, Diabetes ≥ 200) ----

    @Test
    fun postMeal_normalAtBoundaries() {
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(70.0, MealContext.AFTER_MEAL_2H))
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(120.0, MealContext.AFTER_MEAL_2H))
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(139.0, MealContext.AFTER_MEAL_2H))
    }

    @Test
    fun postMeal_prediabetesAtBoundaries() {
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(140.0, MealContext.AFTER_MEAL_2H))
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(175.0, MealContext.AFTER_MEAL_2H))
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(199.0, MealContext.AFTER_MEAL_2H))
    }

    @Test
    fun postMeal_diabetesAtOrAbove200() {
        assertEquals(BloodGlucoseCategory.DIABETES, evaluateBloodGlucose(200.0, MealContext.AFTER_MEAL_2H))
        assertEquals(BloodGlucoseCategory.DIABETES, evaluateBloodGlucose(250.0, MealContext.AFTER_MEAL_2H))
    }

    // ---- Unspecified Meal Context (Default general thresholds: Normal < 140, Prediabetes 140–199, Diabetes ≥ 200) ----

    @Test
    fun unspecifiedContext_normalUnder140() {
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(95.0, null))
        assertEquals(BloodGlucoseCategory.NORMAL, evaluateBloodGlucose(139.0, null))
    }

    @Test
    fun unspecifiedContext_prediabetesBetween140And199() {
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(140.0, null))
        assertEquals(BloodGlucoseCategory.PREDIABETES, evaluateBloodGlucose(199.0, null))
    }

    @Test
    fun unspecifiedContext_diabetesAtOrAbove200() {
        assertEquals(BloodGlucoseCategory.DIABETES, evaluateBloodGlucose(200.0, null))
        assertEquals(BloodGlucoseCategory.DIABETES, evaluateBloodGlucose(260.0, null))
    }
}
